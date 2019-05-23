package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.concurrent.Executors

class LoadTestUserCreationIT {

    private val dataset: Dataset = URI("https://s3-eu-central-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-dah44h6l1l8p/")
        .resolve("jsw-7.13.0-100k-users-sync/")
        .let { bucket ->
            Dataset(
                database = Jperf425WorkaroundMysqlDatabase(MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = Duration.ofMinutes(6)
                    )
                )),
                jiraHomeSource = JiraHomePackage(HttpDatasetPackage(
                    uri = bucket.resolve("jirahome.tar.bz2"),
                    downloadTimeout = Duration.ofMinutes(6)
                )),
                label = "100k users"
            )
        }

    private val behavior = VirtualUserBehavior.Builder(TracingScenario::class.java)
        .createUsers(true)
        .browser(LoadTestTest.TestBrowser::class.java)
        .skipSetup(true)

    @Test
    fun shouldCreateUsersInParallelDespiteBigUserBase() {
        val pool = Executors.newCachedThreadPool()
        val nodes = 6
        val load = VirtualUserLoad.Builder()
            .virtualUsers(75)
            .ramp(Duration.ofSeconds(45))
            .flat(Duration.ofSeconds(2))
            .maxOverallLoad(TemporalRate(1.0, Duration.ofSeconds(1)))
            .build()
        val loadSlices = load.slice(nodes)

        DockerJiraFormula(dataset).runWithJira { jira ->
            (0 until nodes)
                .map { loadSlices[it] }
                .map { loadTest(jira, it) }
                .map { pool.submit { it.run() } }
                .map { it.get() }
        }

        pool.shutdownNow()
    }

    private fun loadTest(
        jira: DockerJira,
        load: VirtualUserLoad
    ): LoadTest = LoadTest(
        options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = jira.hostAddress,
                userName = "admin",
                password = "admin"
            ),
            behavior = behavior
                .load(load)
                .build()
        )
    )
}
