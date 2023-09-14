package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
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

// TODO rename and put into API
/**
 * Applies load on a Jira via page objects. Explores the instance to learn about data and choose pages to visit.
 * Wanders preset Jira pages with different proportions of each page. Their order is random.
 */
internal class ExploratoryVirtualUser(
    private val load: VirtualUserLoad,
    private val taskMeter: ActionMeter,
    private val actions: Iterable<Action>,
    private val diagnostics: Diagnostics
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    fun hold() {
        logger.info("Waiting for ${load.hold}")
        sleep(load.hold.toMillis())
    }

    /**
     * Repeats [actions] until [done] is `true`.
     */
    fun applyLoad(
        done: AtomicBoolean
    ) {
        logger.info("Applying load...")
        val actionNames = actions.map { it.javaClass.simpleName }
        logger.debug("Circling through $actionNames")
        var actionsPerformed = 0.0
        val start = now()
        for (action in CircularIterator(actions)) {
            if (done.get()) {
                logger.info("Done applying load")
                break
            }
            try {
                runWithDiagnostics(action)
            } catch (e: Exception) {
                logger.error("Failed to run $action, but we keep running", e)
            } finally {
                actionsPerformed++
                val expectedTimeSoFar = load.maxOverallLoad.scaleChange(actionsPerformed).time
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
