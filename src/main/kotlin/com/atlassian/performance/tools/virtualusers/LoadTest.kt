package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.AbruptExecutorService
import com.atlassian.performance.tools.concurrency.api.finishBy
import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.jiraactions.api.measure.output.ThrowawayActionMetricOutput
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.diagnostics.DiagnosisLimit
import com.atlassian.performance.tools.virtualusers.api.diagnostics.DiagnosisPatience
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.api.diagnostics.ImpatientDiagnostics
import com.atlassian.performance.tools.virtualusers.api.diagnostics.LimitedDiagnostics
import com.atlassian.performance.tools.virtualusers.api.diagnostics.WebDriverDiagnostics
import com.atlassian.performance.tools.virtualusers.api.users.UserGenerator
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import com.atlassian.performance.tools.virtualusers.measure.WebJiraNode
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.remote.RemoteWebDriver
import java.nio.file.Path
import java.time.Duration
import java.time.Instant.now
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [load test](https://en.wikipedia.org/wiki/Load_testing).
 */
internal class LoadTest(
    private val options: VirtualUserOptions
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val behavior = options.behavior
    private val target = options.target
    private val nodeResult = VirtualUserNodeResult(options.behavior.results)
    private val nodeCounter = JiraNodeCounter()
    private val random = SeededRandom(behavior.seed)
    private val diagnosisPatience = DiagnosisPatience(Duration.ofSeconds(5))
    private val diagnosisLimit = DiagnosisLimit(behavior.diagnosticsLimit)
    private val scenario = behavior.scenario.getConstructor().newInstance() as Scenario
    private val browser = behavior.browser.getConstructor().newInstance() as Browser
    private val userGenerator = options.behavior.userGenerator.getConstructor().newInstance() as UserGenerator

    private val systemUsers = listOf(
        User(
            name = target.userName,
            password = target.password
        )
    )
    private val load = behavior.load

    fun run(): VirtualUserNodeResult {
        val users = generateUsers()
        logger.info("Holding for ${load.hold}.")
        Thread.sleep(load.hold.toMillis())
        setUpJira()
        applyLoad(users)
        val nodesDump = nodeResult.nodeDistribution.toFile()
        nodesDump.ensureParentDirectory().bufferedWriter().use {
            nodeCounter.dump(it)
        }
        logger.debug("Dumped node's counts to $nodesDump")
        return nodeResult
    }

    private fun generateUsers(): List<User> {
        val deadline = now() + behavior.maxOverhead
        return AbruptExecutorService(
            Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "user-generation-${runnable.hashCode()}")
            }
        ).use { pool ->
            (1..load.virtualUsers)
                .map { pool.submit(Callable { userGenerator.generateUser(options) }) }
                .map { it.finishBy(deadline, logger) }
        }
    }

    private fun setUpJira() {
        if (behavior.skipSetup) {
            logger.debug("Skipped the setup")
            return
        }
        CloseableThreadContext.push("setup").use {
            browser.start().use { closeableDriver ->
                val driver = closeableDriver.getDriver()
                val throwawayMeter = ActionMeter.Builder(ThrowawayActionMetricOutput()).build()
                val vuResult = nodeResult.isolateVuResult("setup")
                createVirtualUser(
                    jira = WebJira(
                        driver = driver,
                        base = target.webApplication,
                        adminPassword = target.password
                    ),
                    meter = throwawayMeter,
                    userMemory = AdaptiveUserMemory(random).apply { remember(systemUsers) },
                    diagnostics = driver.getDiagnostics(vuResult.getDiagnoses())
                ).setUpJira()
            }
        }
    }

    private fun applyLoad(
        users: List<User>
    ) {
        val userCount = users.size
        val finish = load.ramp + load.flat
        val loadPool = ThreadPoolExecutor(
            userCount,
            userCount,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>(),
            ThreadFactoryBuilder().setNameFormat("virtual-user-%d").setDaemon(true).build()
        )
        logger.info("Segmenting load across $userCount VUs")
        val segments = users.mapIndexed { index, user -> segmentLoad(user, index + 1) }
        logger.info("Load segmented")
        segments.forEach { loadPool.submit { applyLoad(it) } }
        Thread.sleep(finish.toMillis())
        close(segments)
    }

    private fun segmentLoad(
        user: User,
        index: Int
    ): LoadSegment {
        val uuid = UUID.randomUUID()
        val vuResult = nodeResult.isolateVuResult(uuid.toString())
        val driver = browser.start()
        return LoadSegment(
            driver = driver,
            diagnostics = driver.getDriver().getDiagnostics(vuResult.getDiagnoses()),
            output = vuResult.writeMetrics(),
            done = AtomicBoolean(false),
            id = uuid,
            index = index,
            user = user
        )
    }

    private fun applyLoad(
        segment: LoadSegment
    ) {
        CloseableThreadContext.push("applying load #${segment.id}").use {
            val rampUpWait = load.rampInterval.multipliedBy(segment.index.toLong())
            logger.info("Waiting for $rampUpWait")
            Thread.sleep(rampUpWait.toMillis())
            createVirtualUser(
                jira = WebJira(
                    driver = segment.driver.getDriver(),
                    base = target.webApplication,
                    adminPassword = target.password
                ),
                meter = ActionMeter.Builder(AppendableActionMetricOutput(segment.output))
                    .virtualUser(segment.id)
                    .build(),
                userMemory = AdaptiveUserMemory(random).apply {
                    remember(
                        listOf(segment.user)
                    )
                },
                diagnostics = segment.diagnostics
            ).applyLoad(segment.done)
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

    private fun close(
        segments: List<LoadSegment>
    ) {
        logger.info("Closing segments")
        val closePool = Executors.newCachedThreadPool { Thread(it, "close-segment") }
        segments
            .map { closePool.submit { it.close() } }
            .forEach { it.get() }
        logger.info("Segments closed")
        closePool.shutdown()
    }

    private fun RemoteWebDriver.getDiagnostics(diagnoses: Path): Diagnostics {
        return LimitedDiagnostics(
            ImpatientDiagnostics(
                WebDriverDiagnostics(this, this, diagnoses),
                diagnosisPatience
            ),
            diagnosisLimit
        )
    }
}
