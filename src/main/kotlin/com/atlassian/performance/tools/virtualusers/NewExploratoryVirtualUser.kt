package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
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
internal class NewExploratoryVirtualUser(
    private val node: ApplicationNode,
    private val nodeCounter: JiraNodeCounter,
    private val actions: Iterable<Action>,
    private val maxLoad: TemporalRate
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    /**
     * Repeats [actions] until [done] is `true`.
     */
    fun applyLoad(
        done: AtomicBoolean
    ) {
        logger.info("Applying load...")
        nodeCounter.count(node)
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
                action.run()
                actionsPerformed++
                val expectedTimeSoFar = maxLoad.scaleChange(actionsPerformed).time
                val actualTimeSoFar = Duration.between(start, now())
                val extraTime = expectedTimeSoFar - actualTimeSoFar
                if (extraTime > Duration.ZERO) {
                    Thread.sleep(extraTime.toMillis())
                }
            } catch (e: Exception) {
                logger.error("Failed to run $action, but we keep running", e)
            }
        }
    }

}
