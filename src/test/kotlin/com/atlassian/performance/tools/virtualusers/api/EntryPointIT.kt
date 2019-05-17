package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.virtualusers.ChromeContainer
import com.atlassian.performance.tools.virtualusers.DockerJira
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URI
import java.nio.file.Paths
import java.time.Duration
import java.util.*

class EntryPointIT {

    private val smallDataset = URI("https://s3-eu-west-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-1sjxdtrv5hdhj/")
        .resolve("af4c7d3b-925c-464c-ab13-79f615158316/")
        .let { bucket ->
            Dataset(
                database = Jperf425WorkaroundMysqlDatabase(MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = Duration.ofMinutes(5)
                    )
                )),
                jiraHomeSource = JiraHomePackage(HttpDatasetPackage(
                    uri = bucket.resolve("jirahome.tar.bz2"),
                    downloadTimeout = Duration.ofMinutes(5)
                )),
                label = "7k issues"
            )
        }

    @Test
    fun shouldRunWith3_2_0_Args() {
        val resultPath = Paths.get("build")
            .resolve("vu-node-result")
            .resolve(UUID.randomUUID().toString())
        val desiredTotalTime = Duration.ofMinutes(2)
        DockerJira(smallDataset).runWithJira { jira: URI ->
            com.atlassian.performance.tools.virtualusers.api.main(arrayOf(
                "--jira-address", jira.toString(),
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
                "--result", resultPath.toString()
            ))
        }

        val vuResult = VirtualUserNodeResult(resultPath)
            .listResults()
            .last()
        val scenarioLabels = vuResult
            .streamScenarioMetrics()
            .map { it.label }
        assertThat(scenarioLabels).containsOnly("Log In", "View Issue")
    }
}
