package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.representsInterrupt
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jvmtasks.api.Backoff
import com.atlassian.performance.tools.jvmtasks.api.IdempotentAction
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.collections.CircularIterator
import com.atlassian.performance.tools.virtualusers.measure.ApplicationNode
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.Instant.now
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Applies load on a Jira via page objects. Explores the instance to learn about data and choose pages to visit.
 * Wanders preset Jira pages with different proportions of each page. Their order is random.
 */
internal class ExploratoryVirtualUser(
    private val node: ApplicationNode,
    private val nodeCounter: JiraNodeCounter,
    private val actions: Iterable<Action>,
    private val setUpAction: Action,
    private val logInAction: Action,
    private val maxLoad: TemporalRate,
    private val diagnostics: Diagnostics
) {
    companion object {
        val shutdown = AtomicBoolean()
    }

    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val loginRetryLimit: Int = 1_000_000

    fun setUpJira() {
        logger.info("Setting up Jira...")
        runWithDiagnostics(logInAction)
        runWithDiagnostics(setUpAction)
        logger.info("Jira is set up")
    }

    /**
     * Repeats [actions] until the thread is interrupted.
     */
    fun applyLoad() {
        shutdown.set(false)
        logger.info("Applying load...")
        logIn()
        nodeCounter.count(node)
        val actionNames = actions.map { it.javaClass.simpleName }
        logger.debug("Circling through $actionNames")
        var actionsPerformed = 0.0
        val start = now()
        for (action in CircularIterator(actions)) {
            try {
                runWithDiagnostics(action)
                actionsPerformed++
                val expectedTimeSoFar = maxLoad.scaleChange(actionsPerformed).time
                val actualTimeSoFar = Duration.between(start, now())
                val extraTime = expectedTimeSoFar - actualTimeSoFar
                if (extraTime > Duration.ZERO) {
                    Thread.sleep(extraTime.toMillis())
                }
            } catch (e: Exception) {
                if (!e.representsInterrupt()) {
                    logger.error("Failed to run $action, but we keep running", e)
                }
            }
            if (ExploratoryVirtualUser.shutdown.get()) {
                clearInterruptedState()
                logger.info("Scenario finished on cue")
                break
            }
        }
    }

    /**
     * We use interruption to break thread's flow. At this point, we no longer care about the interrupted state.
     * We rely on ExploratoryVirtualUser.shutdown instead. We switched to The ExploratoryVirtualUser.shutdown
     * because interrupted state is out of our control (a 3rd party code may erase it).
     */
    private fun clearInterruptedState() {
        Thread.interrupted()
    }

    private fun logIn() {
        IdempotentAction("log in") {
            runWithDiagnostics(logInAction)
        }.retry(
            backoff = StaticBackoff(Duration.ofSeconds(5)),
            maxAttempts = loginRetryLimit
        )
    }

    private fun runWithDiagnostics(
        action: Action
    ) {
        try {
            logger.trace("Running $action")
            action.run()
        } catch (e: Exception) {
            if (e.representsInterrupt().not()) {
                diagnostics.diagnose(e)
            }
            throw Exception("Failed to run $action", e)
        }
    }

}

private class StaticBackoff(
    private val backOff: Duration
) : Backoff {
    override fun backOff(attempt: Int): Duration = backOff
}
