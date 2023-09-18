package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.ExploratoryVirtualUser
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.config.*
import com.atlassian.performance.tools.virtualusers.api.diagnostics.*
import com.atlassian.performance.tools.virtualusers.api.users.UserGenerator
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier


class ScenarioToLoadProcessAdapter : LoadProcess {

    private val setUpActionsRan: AtomicBoolean = AtomicBoolean(false)

    override fun setUp(container: LoadProcessContainer): LoadThreadFactory {
        val options = container.options()
        val behavior = options.behavior
        val scenario = behavior.scenario.getConstructor().newInstance()
        val userGenerator = behavior.userGenerator.getConstructor().newInstance()
        val browser = behavior.browser.getConstructor().newInstance()
        val counter = ClusterNodeCounter()
        container.addCloseable(AutoCloseable {
            container.result().writeNodeCounts().use { counter.dump(it) }
        })
        return ScenarioToLoadThreadAdapterFactory(scenario, userGenerator, browser, setUpActionsRan, counter)
    }
}

private class ScenarioToLoadThreadAdapterFactory(
    private val scenario: Scenario,
    private val userGenerator: UserGenerator,
    private val browser: Browser,
    private val setUpActionRan: AtomicBoolean,
    private val nodeCounter: ClusterNodeCounter
) : LoadThreadFactory {

    private val logger: Logger = LogManager.getLogger(this::class.java)

    override fun fireUp(container: LoadThreadContainer): LoadThread {
        val options = container.loadProcessContainer().options()
        val behavior = options.behavior
        val closeableWebDriver = browser.start()
        container.addCloseable(closeableWebDriver)
        val webDriver = closeableWebDriver.getDriver()
        val target = options.target
        val webJira = WebJira(
            webDriver,
            target.webApplication,
            target.password
        )
        val random = SeededRandom(container.random().nextLong())
        val userMemory = AdaptiveUserMemory(random).also {
            val generatedUser = userGenerator.generateUser(options)
            it.remember(listOf(generatedUser))
        }
        val meter = container.actionMeter()
        val diagnostics = LimitedDiagnostics(
            ImpatientDiagnostics(
                WebDriverDiagnostics(webDriver, webDriver, container.threadResult().getDiagnoses()),
                DiagnosisPatience(Duration.ofSeconds(5))
            ),
            DiagnosisLimit(behavior.diagnosticsLimit)
        )
        val userLogin = scenario.getLogInAction(webJira, meter, userMemory)
        val looper = ExploratoryVirtualUser(
            actions = scenario.getActions(webJira, random, meter),
            load = container.singleThreadLoad(),
            taskMeter = container.taskMeter(),
            diagnostics = diagnostics
        )
        setUpOnce(behavior, webJira, meter, target, looper)
        return object : LoadThread {
            override fun generateLoad(
                stop: AtomicBoolean
            ) {
                nodeCounter.count(Supplier {
                    webJira.getJiraNode()
                })
                looper.hold()
                looper.runWithDiagnostics(userLogin)
                looper.applyLoad(stop)
            }
        }
    }

    private fun setUpOnce(
        behavior: VirtualUserBehavior,
        webJira: WebJira,
        meter: ActionMeter,
        target: VirtualUserTarget,
        looper: ExploratoryVirtualUser
    ) {
        if (!behavior.skipSetup) {
            synchronized(setUpActionRan) {
                if (!setUpActionRan.get()) {
                    val adminLogin = scenario.getLogInAction(webJira, meter, AdminUserMemory(target))
                    val setUpAction = scenario.getSetupAction(webJira, meter)
                    logger.info("Setting up Jira...")
                    looper.runWithDiagnostics(adminLogin)
                    looper.runWithDiagnostics(setUpAction)
                    logger.info("Jira is set up")
                    setUpActionRan.set(true)
                }
            }
        }
    }

    private class AdminUserMemory(
        private val target: VirtualUserTarget
    ) : UserMemory {
        override fun recall(): User {
            return User(target.userName, target.password)
        }

        override fun remember(memories: Collection<User>) {
        }
    }
}
