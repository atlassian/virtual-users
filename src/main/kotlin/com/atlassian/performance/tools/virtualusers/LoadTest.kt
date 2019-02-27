package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.TraceableFuture
import com.atlassian.performance.tools.concurrency.api.TraceableTask
import com.atlassian.performance.tools.concurrency.api.finishBy
import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.diagnostics.*
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import com.atlassian.performance.tools.virtualusers.measure.WebJiraNode
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebDriver
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant.now
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A [load test](https://en.wikipedia.org/wiki/Load_testing).
 */
internal class LoadTest(
    options: VirtualUserOptions,
    private val userGenerator: UserGenerator
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val behavior = options.behavior
    private val target = options.target
    private val workspace = Paths.get("test-results")
    private val nodeCounter = JiraNodeCounter()
    private val random = SeededRandom(behavior.seed)
    private val diagnosisPatience = DiagnosisPatience(Duration.ofSeconds(5))
    private val diagnosisLimit = DiagnosisLimit(behavior.diagnosticsLimit)
    private val scenario = behavior.scenario.getConstructor().newInstance() as Scenario
    private val browser = behavior.browser.getConstructor().newInstance() as Browser

    private val systemUsers = listOf(
        User(
            name = target.userName,
            password = target.password
        )
    )
    private val load = behavior.load

    fun run() {
        logger.info("Holding for ${load.hold}.")
        Thread.sleep(load.hold.toMillis())
        workspace.toFile().ensureDirectory()
        setUpJira()
        applyLoad(chooseUsers())
        val nodesDump = workspace.resolve("nodes.csv")
        nodesDump.toFile().bufferedWriter().use {
            nodeCounter.dump(it)
        }
        logger.debug("Dumped node's counts to $nodesDump")
    }

    private fun setUpJira() {
        if (behavior.skipSetup) {
            logger.debug("Skipped the setup")
            return
        }
        CloseableThreadContext.push("setup").use {
            browser.start().use { closeableDriver ->
                val (driver, diagnostics) = closeableDriver.getDriver().toDiagnosableDriver()
                val meter = ActionMeter(virtualUser = UUID.randomUUID())
                val jira = WebJira(
                    driver = driver,
                    base = target.webApplication,
                    adminPassword = target.password
                )
                val userMemory = AdaptiveUserMemory(random)
                userMemory.remember(systemUsers)
                val virtualUser = createVirtualUser(jira, meter, userMemory, diagnostics)
                virtualUser.setUpJira()
            }
        }
    }

    private fun chooseUsers(): List<User> = if (behavior.createUsers) {
        userGenerator.generateUsers(load.virtualUsers)
    } else {
        systemUsers
    }

    private fun applyLoad(users: List<User>) {
        val virtualUsers = load.virtualUsers
        val finish = load.ramp + load.flat
        val maxStop = Duration.ofMinutes(2)
        val loadPool = ThreadPoolExecutor(
            virtualUsers,
            virtualUsers,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>(),
            ThreadFactoryBuilder().setNameFormat("virtual-user-%d").setDaemon(true).build()
        )
        val stopSchedule = Executors.newScheduledThreadPool(
            1,
            ThreadFactoryBuilder().setNameFormat("deferred-stop").setDaemon(true).build()
        )
        val stop = stopSchedule.schedule(
            {
                val active = loadPool.activeCount
                logger.info("Stopping load")
                loadPool.shutdownNow()
                ExploratoryVirtualUser.shutdown.set(true)
                if (active != virtualUsers) {
                    throw Exception("Expected $virtualUsers VUs to still be active, but encountered $active")
                }
            },
            finish.toMillis(),
            TimeUnit.MILLISECONDS
        )
        val deadline = now() + finish + maxStop
        logger.info("Deadline for tests is $deadline.")

        (1..virtualUsers)
            .mapIndexed { virtualUserIndex: Int, _ ->
                val user = users[virtualUserIndex % users.size]
                val task = TraceableTask { applyLoad(virtualUserIndex.toLong(), user) }
                val future = loadPool.submit(task)
                return@mapIndexed TraceableFuture(task, future)
            }
            .forEach { it.finishBy(deadline, logger) }
        stop.finishBy(deadline, logger)
        stopSchedule.shutdownNow()
    }

    private fun applyLoad(
        vuOrder: Long,
        newUser: User
    ) {
        val uuid = UUID.randomUUID()
        CloseableThreadContext.push("applying load #$uuid").use {

            val rampUpWait = load.rampInterval.multipliedBy(vuOrder)
            logger.info("Waiting for $rampUpWait")
            Thread.sleep(rampUpWait.toMillis())

            workspace
                .resolve(uuid.toString())
                .toFile()
                .ensureDirectory()
                .resolve("action-metrics.jpt")
                .bufferedWriter()
                .use { output -> applyLoad(output, uuid, newUser) }
        }
    }

    private fun applyLoad(
        output: Appendable,
        uuid: UUID,
        newUser: User
    ) {
        browser.start().use { closeableDriver ->
            val (driver, diagnostics) = closeableDriver.getDriver().toDiagnosableDriver()
            val jira = WebJira(
                driver = driver,
                base = target.webApplication,
                adminPassword = target.password
            )
            val userMemory = AdaptiveUserMemory(random)
            userMemory.remember(
                listOf(
                    User(
                        name = newUser.name,
                        password = newUser.password
                    )
                )
            )
            val meter = ActionMeter(
                virtualUser = uuid,
                output = AppendableActionMetricOutput(output)
            )
            val virtualUser = createVirtualUser(jira, meter, userMemory, diagnostics)
            virtualUser.applyLoad()
        }
    }

    private fun createVirtualUser(
        jira: WebJira,
        meter: ActionMeter,
        userMemory: UserMemory,
        diagnostics: Diagnostics
    ): ExploratoryVirtualUser {
        val scenarioAdapter = ScenarioAdapter(scenario)
        val maxOverallLoad = load.maxOverallLoad
        return ExploratoryVirtualUser(
            node = WebJiraNode(jira),
            nodeCounter = nodeCounter,
            actions = scenarioAdapter.getActions(
                jira = jira,
                seededRandom = SeededRandom(random.random.nextLong()),
                meter = meter
            ),
            setUpAction = scenarioAdapter.getSetupAction(
                jira = jira,
                meter = meter
            ),
            logInAction = scenarioAdapter.getLogInAction(
                jira = jira,
                meter = meter,
                userMemory = userMemory
            ),
            maxLoad = maxOverallLoad / load.virtualUsers,
            diagnostics = diagnostics
        )
    }

    private fun RemoteWebDriver.toDiagnosableDriver(): DiagnosableDriver {
        return DiagnosableDriver(
            this,
            LimitedDiagnostics(
                ImpatientDiagnostics(
                    WebDriverDiagnostics(this),
                    diagnosisPatience
                ),
                diagnosisLimit
            )
        )
    }

    internal data class DiagnosableDriver(
        val driver: WebDriver,
        val diagnostics: Diagnostics
    )
}