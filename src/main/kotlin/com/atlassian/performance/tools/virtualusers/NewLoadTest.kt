package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.diagnostics.*
import com.atlassian.performance.tools.virtualusers.lib.api.Scenario
import com.atlassian.performance.tools.virtualusers.measure.ApplicationNode
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.BufferedWriter
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [load test](https://en.wikipedia.org/wiki/Load_testing).
 */
internal class NewLoadTest(
    private val options: VirtualUserOptions
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val behavior = options.behavior
    private val target = options.target
    private val workspace = Paths.get("test-results")
    private val nodeCounter = JiraNodeCounter()
    private val diagnosisPatience = DiagnosisPatience(Duration.ofSeconds(5))
    private val diagnosisLimit = DiagnosisLimit(behavior.diagnosticsLimit)


    private fun createScenario(virtualUserTarget: VirtualUserTarget, actionMeter: ActionMeter): Scenario {
        return behavior
            .scenario
            .getConstructor(VirtualUserTarget::class.java, ActionMeter::class.java)
            .newInstance(
                virtualUserTarget,
                actionMeter
            ) as Scenario
    }

    private val load = behavior.load

    fun run() {
        logger.info("Holding for ${load.hold}.")
        Thread.sleep(load.hold.toMillis())
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
            val createScenario = createScenario(
                virtualUserTarget = target,
                actionMeter = ActionMeter(virtualUser = UUID.randomUUID())
            )
            createScenario.setup()
            createScenario.cleanUp()
        }
    }

    private fun applyLoad() {
        val userCount = load.virtualUsers
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
        val segments = (0..userCount).map { index ->
            segmentLoad(index + 1)
        }
        logger.info("Load segmented")
        segments.forEach { loadPool.submit { applyLoad(it) } }
        Thread.sleep(finish.toMillis())
        close(segments)
    }

    private fun segmentLoad(
        index: Int
    ): NewLoadSegment {
        val uuid = UUID.randomUUID()
        val output = output(uuid)
        val scenario = createScenario(options.target,
            ActionMeter(
                virtualUser = uuid,
                output = AppendableActionMetricOutput(output)
            )
        )

        return NewLoadSegment(
            scenario = scenario,
            output = output,
            done = AtomicBoolean(false),
            id = uuid,
            index = index
        )
    }

    private fun output(uuid: UUID): BufferedWriter {
        return workspace
            .resolve(uuid.toString())
            .toFile()
            .ensureDirectory()
            .resolve("action-metrics.jpt")
            .bufferedWriter()
    }

    private fun applyLoad(
        segment: NewLoadSegment
    ) {
        CloseableThreadContext.push("applying load #${segment.id}").use {
            val rampUpWait = load.rampInterval.multipliedBy(segment.index.toLong())
            logger.info("Waiting for $rampUpWait")
            Thread.sleep(rampUpWait.toMillis())
            val virtualUser = createVirtualUser(segment)
            segment.scenario.before()
            virtualUser.applyLoad(segment.done)
        }
    }

    private fun createVirtualUser(
        segment: NewLoadSegment
    ): NewExploratoryVirtualUser {
        val maxOverallLoad = load.maxOverallLoad
        return NewExploratoryVirtualUser(
            node = object : ApplicationNode {
                override fun identify(): String = "todo??"
            },
            nodeCounter = nodeCounter,
            actions = segment.scenario.actions,
            maxLoad = maxOverallLoad / load.virtualUsers
        )
    }

    private fun close(
        segments: List<AutoCloseable>
    ) {
        logger.info("Closing segments")
        val closePool = Executors.newCachedThreadPool { Thread(it, "close-segment") }
        segments
            .map { closePool.submit { it.close() } }
            .forEach { it.get() }
        logger.info("Segments closed")
        closePool.shutdown()
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
