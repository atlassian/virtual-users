package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.virtualusers.ChromeContainer
import com.atlassian.performance.tools.virtualusers.DockerJiraFormula
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import com.atlassian.performance.tools.virtualusers.TestVuNode
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.function.Predicate
import kotlin.streams.toList

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
    fun shouldRunWith3_2_0_Args() {
        val resultPath = TestVuNode.isolateTestNode(javaClass)
        jiraFormula.runWithJira { jira ->
            com.atlassian.performance.tools.virtualusers.api.main(arrayOf(
                "--jira-address", jira.peerAddress.toString(),
                "--login", "admin",
                "--password", "admin",
                "--virtual-users", "1",
                "--hold", "PT0S",
                "--ramp", "PT0S",
                "--flat", "PT2M",
                "--scenario", SimpleScenario::class.java.name,
                "--browser", ChromeContainer::class.java.name,
                "--results", resultPath.toString(),
                "--diagnostics-limit", "64",
                "--seed", "-9183767962456348780"
            ))
        }
        val vuResult = VirtualUserNodeResult(resultPath)
            .listResults()
            .last()
        val metrics = vuResult.streamMetrics().toList()
        assertThat(metrics)
            .extracting<String> { it.label }
            .containsOnly("Log In", "View Issue")
        val ok = Condition<ActionMetric>(Predicate { it.result == ActionResult.OK }, "OK")
        assertThat(metrics).haveAtLeast(2, ok)
    }
}
