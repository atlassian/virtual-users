package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.virtualusers.ChromeContainer
import com.atlassian.performance.tools.virtualusers.DockerJiraFormula
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import com.atlassian.performance.tools.virtualusers.TestVuNode
import com.atlassian.performance.tools.virtualusers.api.ActivityType.ACTING
import com.atlassian.performance.tools.virtualusers.api.ActivityType.DIAGNOSING
import com.atlassian.performance.tools.virtualusers.api.ActivityType.MYSTERY
import com.atlassian.performance.tools.virtualusers.api.ActivityType.THROTTLING
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.stream.Stream
import kotlin.streams.asSequence

class EntryPointIT {

    private val smallDataset = URI("https://s3-eu-west-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-1sjxdtrv5hdhj/")
        .resolve("dataset-f8dba866-9d1b-492e-b76c-f4a78ac3958c/")
        .let { bucket ->
            Dataset(
                label = "7k issues JSW 7.2.0",
                database = Jperf425WorkaroundMysqlDatabase(MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = Duration.ofMinutes(5)
                    )
                )),
                jiraHomeSource = JiraHomePackage(HttpDatasetPackage(
                    uri = bucket.resolve("jirahome.tar.bz2"),
                    downloadTimeout = Duration.ofMinutes(5)
                ))
            )
        }
    private val jiraFormula = DockerJiraFormula(Jperf424WorkaroundJswDistro("7.2.0"), smallDataset)

    @Test
    fun shouldRun() {
        val resultPath = TestVuNode.isolateTestNode(javaClass)
        val desiredTotalTime = Duration.ofMinutes(2)
        jiraFormula.runWithJira { jira ->
            com.atlassian.performance.tools.virtualusers.api.main(arrayOf(
                "--jira-address", jira.peerAddress.toString(),
                "--login", "admin",
                "--password", "admin",
                "--virtual-users", "1",
                "--hold", "PT0S",
                "--ramp", "PT0S",
                "--flat", desiredTotalTime.toString(),
                "--scenario", SimpleScenario::class.java.name,
                "--browser", ChromeContainer::class.java.name,
                "--diagnostics-limit", "3",
                "--seed", "-9183767962456348780",
                "--results", resultPath.toString(),
                "--max-overall-load", "1.0/PT5S"
            ))
        }
        val vuResult = VirtualUserNodeResult(resultPath)
            .listResults()
            .last()
        val scenarioLabels = vuResult
            .streamScenarioMetrics()
            .map { it.label }
        assertThat(scenarioLabels).containsOnly("Log In", "View Issue")
        val activityTypes = vuResult
            .streamActivityMetrics()
            .map { it.type }
        assertThat(activityTypes).contains(ACTING, THROTTLING, DIAGNOSING).doesNotContain(MYSTERY)
        val totalActivityTime = vuResult
            .streamActivityMetrics()
            .map { it.metric }
            .sumDurations()
        val unaccountedTime = desiredTotalTime - totalActivityTime
        assertThat(unaccountedTime).isLessThan(Duration.ofSeconds(5))
        val totalMetricsTime = vuResult

            .streamScenarioMetrics()
            .sumDurations()
        val totalActingTime = vuResult
            .streamActivityMetrics()
            .filter { it.type == ACTING }
            .map { it.metric }
            .sumDurations()
        val unmeasuredActingTime = totalActingTime - totalMetricsTime
        assertThat(unmeasuredActingTime).isLessThan(Duration.ofSeconds(1))
    }

    private fun Stream<ActionMetric>.sumDurations(): Duration {
        return map { it.duration }
            .asSequence()
            .fold(Duration.ZERO) { a, b -> a + b }
    }
}
