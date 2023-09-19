package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.concurrency.api.AbruptExecutorService
import com.atlassian.performance.tools.concurrency.api.finishBy
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.BestEffortCloseable
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.diagnostics.*
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import net.jcip.annotations.ThreadSafe
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.Instant.now
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class ScenarioLoadProcess : LoadProcess {

    private val logger = LogManager.getLogger(this::class.java)
    private val setUpActionsRan: AtomicBoolean = AtomicBoolean(false)

    override fun prepareFactory(container: LoadProcessContainer): LoadThreadFactory {
        val options = container.options()
        val behavior = options.behavior
        val scenario = behavior.scenario.getConstructor().newInstance()
        val browser = behavior.browser.getConstructor().newInstance()
        val counter = ClusterNodeCounter()
        container.addCloseable(AutoCloseable {
            container.result().writeNodeCounts().use { counter.dump(it) }
        })
        val users = preGenerateUsers(options)
        return ScenarioThreadFactory(scenario, users, browser, setUpActionsRan, counter)
    }

    private fun preGenerateUsers(options: VirtualUserOptions): List<User> {
        val behavior = options.behavior
        val userGenerator = behavior.userGenerator.getConstructor().newInstance()
        val deadline = now() + behavior.maxOverhead
        return AbruptExecutorService(
            Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "user-generation-${runnable.hashCode()}")
            }
        ).use { pool ->
            (1..behavior.load.virtualUsers)
                .map { pool.submit(Callable { userGenerator.generateUser(options) }) }
                .map { it.finishBy(deadline, logger) }
        }
    }
}


@ThreadSafe
internal class ScenarioThreadFactory(
    private val scenario: Scenario,
    private val users: List<User>,
    private val browser: Browser,
    private val setUpActionRan: AtomicBoolean,
    private val nodeCounter: ClusterNodeCounter
) : LoadThreadFactory {

    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val unallocatedUsers: Queue<User> = ConcurrentLinkedQueue(users)

    companion object {
        internal val DRIVER_CLOSE_TIMEOUT = Duration.ofSeconds(30)
    }

    override fun prepareThread(container: LoadThreadContainer): LoadThread {
        val options = container.loadProcessContainer().options()
        val behavior = options.behavior
        val closeableWebDriver = browser.start()
        container.addCloseable(
            BestEffortCloseable(closeableWebDriver, DRIVER_CLOSE_TIMEOUT, "WebDriver", logger)
        )
        val webDriver = closeableWebDriver.getDriver()
        val target = options.target
        val webJira = WebJira(
            webDriver,
            target.webApplication,
            target.password
        )
        val random = container.seededRandom()
        val userMemory = allocateUser(random)
        val meter = container.actionMeter()
        val diagnostics = LimitedDiagnostics(
            ImpatientDiagnostics(
                WebDriverDiagnostics(webDriver, webDriver, container.threadResult().getDiagnoses()),
                DiagnosisPatience(Duration.ofSeconds(5))
            ),
            DiagnosisLimit(behavior.diagnosticsLimit)
        )
        val userLogin = scenario.getLogInAction(webJira, meter, userMemory)
        val looper = ThrottlingActionLoop(
            actions = scenario.getActions(webJira, random, meter),
            maxLoad = container.singleThreadLoad().maxOverallLoad,
            taskMeter = container.taskMeter(),
            diagnostics = diagnostics
        )
        setUpOnce(behavior, webJira, meter, target, looper)
        return ScenarioThread(webJira, looper, userLogin, nodeCounter)
    }

    private fun allocateUser(random: SeededRandom): UserMemory {
        val memory = AdaptiveUserMemory(random)
        val user = unallocatedUsers.poll()
            ?: throw Exception("Asked for another user but all $users are already taken")
        memory.remember(listOf(user))
        return memory
    }

    private fun setUpOnce(
        behavior: VirtualUserBehavior,
        webJira: WebJira,
        meter: ActionMeter,
        target: VirtualUserTarget,
        looper: ThrottlingActionLoop
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

private class ScenarioThread(
    private val webJira: WebJira,
    private val looper: ThrottlingActionLoop,
    private val userLogin: Action,
    private val nodeCounter: ClusterNodeCounter
) : LoadThread {
    override fun generateLoad(
        stop: AtomicBoolean
    ) {
        nodeCounter.count(Supplier {
            webJira.getJiraNode()
        })
        looper.runWithDiagnostics(userLogin)
        looper.generateLoad(stop)
    }
}
