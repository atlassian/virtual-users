package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.TraceableFuture
import com.atlassian.performance.tools.concurrency.api.TraceableTask
import com.atlassian.performance.tools.concurrency.api.finishBy
import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.LogInAction
import com.atlassian.performance.tools.jiraactions.api.action.SetUpAction
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.ChromedriverRuntime
import com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome
import com.atlassian.performance.tools.virtualusers.measure.*
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.WebDriver
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
    private val options: VirtualUserOptions
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    private val workspace = Paths.get("test-results")
    private val nodeCounter = JiraNodeCounter()
    private val driverRuntime = ChromedriverRuntime()
    private val random = SeededRandom(options.seed)
    private val diagnosisPatience = DiagnosisPatience(Duration.ofSeconds(5))
    private val diagnosisLimit = DiagnosisLimit(options.diagnosticsLimit)

    fun run() {
        workspace.toFile().ensureDirectory()
        setUpJira()
        applyLoad()
        val nodesDump = workspace.resolve("nodes.csv")
        nodesDump.toFile().bufferedWriter().use {
            nodeCounter.dump(it)
        }
        logger.debug("Dumped node's counts to $nodesDump")
    }

    private fun setUpJira() {
        CloseableThreadContext.push("setup").use {
            val (driver, diagnostics) = startChrome()
            val meter = ActionMeter(virtualUser = UUID.randomUUID())
            val jira = WebJira(
                driver = driver,
                base = options.jiraAddress,
                adminPassword = options.adminPassword
            )
            val userMemory = AdaptiveUserMemory(random)
            userMemory.remember(
                listOf(
                    User(
                        name = options.adminLogin,
                        password = options.adminPassword
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

    private fun applyLoad() {
        val load = options.virtualUserLoad
        val virtualUsers = load.virtualUsers
        val finish = load.total
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
                if (active != virtualUsers) {
                    throw Exception("Expected $virtualUsers VUs to still be active, but encountered $active")
                }
            },
            finish.toMillis(),
            TimeUnit.MILLISECONDS
        )
        val deadline = now() + finish + maxStop
        logger.info("Deadline for tests is $deadline.")
        logger.info("Holding for ${load.hold}.")
        Thread.sleep(load.hold.toMillis())

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

            val rampUpWait = options.virtualUserLoad.rampInterval.multipliedBy(vuOrder)
            logger.info("Waiting for $rampUpWait")
            Thread.sleep(rampUpWait.toMillis())

            workspace
                .resolve(uuid.toString())
                .toFile()
                .ensureDirectory()
                .resolve("action-metrics.jpt")
                .bufferedWriter()
                .use { output -> applyLoad(output, uuid) }
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
            base = options.jiraAddress,
            adminPassword = options.adminPassword
        )
        val userMemory = AdaptiveUserMemory(random)
        userMemory.remember(
            listOf(
                User(
                    name = options.adminLogin,
                    password = options.adminPassword
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
            actions = options.scenario.getActions(
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

    internal data class DiagnosableDriver(
        val driver: WebDriver,
        val diagnostics: Diagnostics
    )
}