package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.representsInterrupt
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.action.LogInAction
import com.atlassian.performance.tools.jiraactions.api.action.SetUpAction
import com.atlassian.performance.tools.jvmtasks.api.Backoff
import com.atlassian.performance.tools.jvmtasks.api.IdempotentAction
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.collections.CircularIterator
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.Thread.currentThread
import java.lang.Thread.interrupted
import java.time.Duration

/**
 * Applies load on a Jira via page objects. Explores the instance to learn about data and choose pages to visit.
 * Wanders preset Jira pages with different proportions of each page. Their order is random.
 */
internal class ExploratoryVirtualUser(
    private val jira: WebJira,
    private val nodeCounter: JiraNodeCounter,
    private val actions: Iterable<Action>,
    private val setUpAction: SetUpAction,
    private val logInAction: LogInAction,
    private val diagnostics: Diagnostics
) {
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
        logger.info("Applying load...")
        logIn()
        val node = jira.getJiraNode()
        nodeCounter.count(node)
        val actionNames = actions.map { it.javaClass.simpleName }
        logger.debug("Circling through $actionNames")
        for (action in CircularIterator(actions)) {
            try {
                runWithDiagnostics(action)
            } catch (e: Exception) {
                if (e.representsInterrupt()) {
                    currentThread().interrupt()
                } else {
                    logger.error("Failed to run $action, but we keep running", e)
                }
            }
            if (interrupted()) {
                logger.info("Scenario finished on cue")
                break
            }
        }
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
