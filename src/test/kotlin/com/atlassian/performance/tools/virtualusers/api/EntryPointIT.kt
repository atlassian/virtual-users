package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.virtualusers.DockerJira
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import com.atlassian.performance.tools.virtualusers.TestJira.SMALL_JIRA
import com.atlassian.performance.tools.virtualusers.TestVuNode
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.ACTING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.DIAGNOSING
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.MYSTERY
import com.atlassian.performance.tools.virtualusers.api.VirtualUserTasks.THROTTLING
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import com.atlassian.performance.tools.virtualusers.load.HttpLoadProcess
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import java.time.Duration
import java.util.function.Predicate
import kotlin.streams.toList

class EntryPointIT {

    @Test
    fun shouldProduceBrowserLoadMetrics() {
        val vuCount = 2
        val desiredTotalTime = Duration.ofMinutes(2)
        val resultPath = TestVuNode.isolateTestNode(javaClass)
        SMALL_JIRA.runWithJira { jira ->
            main(
                arrayOf(
                    *jira.toTargetArgs(),
                    "--virtual-users", vuCount.toString(),
                    "--hold", "PT0S",
                    "--ramp", "PT0S",
                    "--flat", desiredTotalTime.toString(),
                    "--max-overall-load", "1.0/PT5S",
                    "--scenario", SimpleScenario::class.java.name,
                    "--browser", HeadlessChromeBrowser::class.java.name,
                    "--results", resultPath.toString(),
                    "--diagnostics-limit", "3",
                    "--seed", "-9183767962456348780"
                )
            )
        }
        val nodeResult = VirtualUserNodeResult(resultPath)
        val tasks = nodeResult.listResults().flatMap { it.streamTasks().toList() }
        val actions = nodeResult.listResults().flatMap { it.streamActions().toList() }
        val unaccountedTime = desiredTotalTime.multipliedBy(vuCount.toLong()) - tasks.sumDurations()
        with(SoftAssertions()) {
            assertThat(actions.map { it.label }.toSet()).containsOnly("Log In", "See System Info", "Set Up")
            assertThat(actions).haveAtLeast(3, isOk())
            assertThat(tasks.map { it.label })
                .contains(ACTING.label, THROTTLING.label, DIAGNOSING.label)
                .doesNotContain(MYSTERY.label)
            assertThat(unaccountedTime).isLessThan(Duration.ofSeconds(5))
            assertThat(nodeResult.nodeDistribution.parent.fileName.toString()).isEqualTo("test-results")
            assertThat(nodeResult.countVusPerNode())
                .hasSize(1)
                .containsValue(2)
            assertAll()
        }
    }

    @Test
    fun shouldProduceHttpLoadMetrics() {
        val resultPath = TestVuNode.isolateTestNode(javaClass)
        SMALL_JIRA.runWithJira { jira ->
            main(
                arrayOf(
                    *jira.toTargetArgs(),
                    "--load-process", HttpLoadProcess::class.java.name,
                    "--virtual-users", "100",
                    "--hold", "PT0S",
                    "--ramp", "PT0S",
                    "--flat", Duration.ofMinutes(2).toString(),
                    "--max-overall-load", "10000.0/PT1S",
                    "--results", resultPath.toString(),
                    "--diagnostics-limit", "5",
                    "--seed", "12345"
                )
            )
        }
        val nodeResult = VirtualUserNodeResult(resultPath)
        val actions = nodeResult.listResults().flatMap { it.streamActions().toList() }
        assertThat(actions.map { it.label }).containsOnly("POST search")
        assertThat(actions).haveAtLeast(100, isOk())
    }

    private fun isOk() = Condition<ActionMetric>(Predicate { it.result == ActionResult.OK }, "OK")

    private fun List<ActionMetric>.sumDurations(): Duration {
        return map { it.duration }.fold(Duration.ZERO) { a, b -> a + b }
    }

    private fun DockerJira.toTargetArgs() = arrayOf(
        "--jira-address", peerAddress.toString(),
        "--login", "admin",
        "--password", "admin"
    )
}
