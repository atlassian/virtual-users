package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.ACTING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.DIAGNOSING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.THROTTLING
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.collections.CircularIterator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant.now
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Respects [maxLoad].
 * Loops through [actions] and uses [diagnostics] when  an action fails.
 * Measures overheads with [taskMeter].
 */
class ThrottlingActionLoop(
    private val maxLoad: TemporalRate,
    private val taskMeter: ActionMeter,
    private val actions: Iterable<Action>,
    private val diagnostics: Diagnostics
) : LoadThread {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    override fun generateLoad(stop: AtomicBoolean) {
        logger.info("Applying load...")
        val actionNames = actions.map { it.javaClass.simpleName }
        logger.debug("Circling through $actionNames")
        var actionsPerformed = 0.0
        val start = now()
        for (action in CircularIterator(actions)) {
            if (stop.get()) {
                logger.info("Done applying load")
                break
            }
            try {
                runWithDiagnostics(action)
            } catch (e: Exception) {
                logger.error("Failed to run $action, but we keep running", e)
            } finally {
                actionsPerformed++
                val expectedTimeSoFar = maxLoad.scaleChange(actionsPerformed).time
                val actualTimeSoFar = Duration.between(start, now())
                val extraTime = expectedTimeSoFar - actualTimeSoFar
                if (extraTime > Duration.ZERO) {
                    taskMeter.measure(THROTTLING) {
                        sleep(extraTime.toMillis())
                    }
                }
            }
        }
    }

    internal fun runWithDiagnostics(
        action: Action
    ) {
        try {
            logger.trace("Running $action")
            taskMeter.measure(ACTING) {
                action.run()
            }
        } catch (e: Exception) {
            taskMeter.measure(DIAGNOSING) {
                diagnostics.diagnose(e)
            }
            throw Exception("Failed to run $action", e)
        }
    }
}
