package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import com.atlassian.performance.tools.virtualusers.TestJira.SMALL_JIRA
import com.atlassian.performance.tools.virtualusers.TestVuNode
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.ACTING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.DIAGNOSING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.MYSTERY
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.THROTTLING
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.Test
import java.time.Duration
import java.util.function.Predicate
import kotlin.streams.toList

class EntryPointIT {

    @Test
    fun shouldProduceMetrics() {
        val desiredTotalTime = Duration.ofMinutes(2)

        val nodeResult = runMain(desiredTotalTime)
        val result = nodeResult.listResults().last()

        val tasks = result.streamTasks().toList()
        val actions = result.streamActions().toList()
        val unaccountedTime = desiredTotalTime - tasks.sumDurations()
        assertThat(actions.map { it.label }).containsOnly("Log In", "See System Info")
        assertThat(actions).haveAtLeast(2, isOk())
        assertThat(tasks.map { it.label })
            .contains(ACTING.label, THROTTLING.label, DIAGNOSING.label)
            .doesNotContain(MYSTERY.label)
        assertThat(unaccountedTime).isLessThan(Duration.ofSeconds(5))
        assertThat(nodeResult.nodeDistribution.parent.fileName.toString()).isEqualTo("test-results")
    }

    private fun runMain(desiredTotalTime: Duration): VirtualUserNodeResult {
        val resultPath = TestVuNode.isolateTestNode(javaClass)
        SMALL_JIRA.runWithJira { jira ->
            main(arrayOf(
                "--jira-address", jira.peerAddress.toString(),
                "--login", "admin",
                "--password", "admin",
                "--virtual-users", "1",
                "--hold", "PT0S",
                "--ramp", "PT0S",
                "--flat", desiredTotalTime.toString(),
                "--max-overall-load", "1.0/PT5S",
                "--scenario", SimpleScenario::class.java.name,
                "--browser", HeadlessChromeBrowser::class.java.name,
                "--results", resultPath.toString(),
                "--diagnostics-limit", "3",
                "--seed", "-9183767962456348780"
            ))
        }
        return VirtualUserNodeResult(resultPath)
    }

    private fun isOk() = Condition<ActionMetric>(Predicate { it.result == ActionResult.OK }, "OK")

    private fun List<ActionMetric>.sumDurations(): Duration {
        return map { it.duration }.fold(Duration.ZERO) { a, b -> a + b }
    }
}
