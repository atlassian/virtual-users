package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.TraceableFuture
import com.atlassian.performance.tools.concurrency.TraceableTask
import com.atlassian.performance.tools.concurrency.finishBy
import com.atlassian.performance.tools.io.ensureDirectory
import com.atlassian.performance.tools.jiraactions.SeededRandom
import com.atlassian.performance.tools.jiraactions.WebJira
import com.atlassian.performance.tools.jiraactions.action.LogInAction
import com.atlassian.performance.tools.jiraactions.action.SetUpAction
import com.atlassian.performance.tools.jiraactions.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.jiraactions.memories.User
import com.atlassian.performance.tools.jiraactions.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jiraactions.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.browsers.ChromedriverRuntime
import com.atlassian.performance.tools.virtualusers.browsers.GoogleChrome
import com.atlassian.performance.tools.virtualusers.measure.*
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.WebDriver
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.util.*
import java.util.concurrent.*

/**
 * A [load test](https://en.wikipedia.org/wiki/Load_testing).
 */
class BasicTest(
    private val jiraAddress: URI,
    private val scenario: Scenario,
    private val adminLogin: String,
    private val adminPassword: String,
    private val random: SeededRandom,
    private val rampUpInterval: Duration
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    private val workspace = Paths.get("test-results")
    private val nodeCounter = JiraNodeCounter()
    private val driverRuntime = ChromedriverRuntime()
    private val metricsFiles: MutableList<File> = CopyOnWriteArrayList()
    private val diagnosisLimit = DiagnosisLimit(64)
    private val diagnosisPatience = DiagnosisPatience(Duration.ofSeconds(5))

    fun run(
        minimumRun: Duration,
        virtualUsers: Int
    ): List<File> {
        val enoughLoad = now() + minimumRun
        workspace.toFile().ensureDirectory()
        setUpJira()
        applyLoad(virtualUsers, enoughLoad, Duration.ofMinutes(2))
        val nodesDump = workspace.resolve("nodes.csv")
        nodesDump.toFile().bufferedWriter().use {
            nodeCounter.dump(it)
        }
        logger.debug("Dumped node's counts to $nodesDump")
        return metricsFiles
    }

    private fun setUpJira() {
        CloseableThreadContext.push("setup").use {
            val (driver, diagnostics) = startChrome()
            val meter = ActionMeter(virtualUser = UUID.randomUUID())
            val jira = WebJira(
                driver = driver,
                base = jiraAddress,
                adminPassword = adminPassword
            )
            val userMemory = AdaptiveUserMemory(random)
            userMemory.remember(
                listOf(
                    User(
                        name = adminLogin,
                        password = adminPassword
                    )
                )
            )
            val virtualUser = createVirtualUser(jira, meter, userMemory, diagnostics)
            try {
                virtualUser.setUpJira()
            } finally {
                driver.quit()
            }
        }
    }

    /**
     * Apply load for at least [enoughLoad] and wait to gracefully stop for at most [maxStop] afterwards.
     */
    private fun applyLoad(
        virtualUsers: Int,
        enoughLoad: Instant,
        maxStop: Duration
    ) {
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
                if (active != virtualUsers) {
                    throw Exception("Expected $virtualUsers VUs to still be active, but encountered $active")
                }
            },
            Duration.between(now(), enoughLoad).toMillis(),
            TimeUnit.MILLISECONDS
        )
        val deadline = enoughLoad + maxStop
        (1..virtualUsers)
            .mapIndexed { i: Int, _ ->
                val task = TraceableTask { applyLoad(i.toLong()) }
                val future = loadPool.submit(task)
                return@mapIndexed TraceableFuture(task, future)
            }
            .forEach { it.finishBy(deadline, logger) }
        stop.finishBy(deadline, logger)
        stopSchedule.shutdownNow()
    }

    private fun applyLoad(
        vuOrder: Long
    ) {
        val uuid = UUID.randomUUID()
        CloseableThreadContext.push("applying load #$uuid").use {

            val rampUpWait = rampUpInterval.multipliedBy(vuOrder)
            logger.info("Waiting for $rampUpWait")
            Thread.sleep(rampUpWait.toMillis())

            val metricsFile = workspace
                .resolve(uuid.toString())
                .toFile()
                .ensureDirectory()
                .resolve("action-metrics.jpt")
            metricsFiles.add(metricsFile)
            metricsFile.bufferedWriter().use { output ->
                applyLoad(output, uuid)
            }
        }
    }

    private fun applyLoad(
        output: Appendable,
        uuid: UUID
    ) {
        val (driver, diagnostics) = try {
            startChrome()
        } catch (e: Exception) {
            logger.error("Failed to start Google Chrome", e)
            return
        }
        val jira = WebJira(
            driver = driver,
            base = jiraAddress,
            adminPassword = adminPassword
        )
        val userMemory = AdaptiveUserMemory(random)
        userMemory.remember(
            listOf(
                User(
                    name = adminLogin,
                    password = adminPassword
                )
            )
        )
        val meter = ActionMeter(
            virtualUser = uuid,
            output = AppendableActionMetricOutput(output)
        )
        val virtualUser = createVirtualUser(jira, meter, userMemory, diagnostics)
        try {
            virtualUser.applyLoad()
        } finally {
            driver.quit()
        }
    }

    private fun createVirtualUser(
        jira: WebJira,
        meter: ActionMeter,
        userMemory: UserMemory,
        diagnostics: Diagnostics
    ): ExploratoryVirtualUser {
        return ExploratoryVirtualUser(
            jira = jira,
            nodeCounter = nodeCounter,
            actions = scenario.getActions(
                jira = jira,
                seededRandom = SeededRandom(random.random.nextLong()),
                meter = meter
            ),
            setUpAction = SetUpAction(
                jira = jira,
                meter = meter
            ),
            logInAction = LogInAction(
                jira = jira,
                meter = meter,
                userMemory = userMemory
            ),
            diagnostics = diagnostics
        )
    }

    private fun startChrome(): DiagnosableDriver {
        val chrome = GoogleChrome(driverRuntime).start()
        chrome.manage().timeouts().pageLoadTimeout(1, TimeUnit.MINUTES)
        return DiagnosableDriver(
            chrome,
            LimitedDiagnostics(
                ImpatientDiagnostics(
                    WebDriverDiagnostics(chrome),
                    diagnosisPatience
                ),
                diagnosisLimit
            )
        )
    }

    data class DiagnosableDriver(
        val driver: WebDriver,
        val diagnostics: Diagnostics
    )
}