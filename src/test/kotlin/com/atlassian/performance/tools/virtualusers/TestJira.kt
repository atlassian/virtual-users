package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import java.net.URI
import java.time.Duration.ofMinutes

internal object TestJira {

    val SMALL_JIRA = URI("https://s3-eu-west-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-1sjxdtrv5hdhj/")
        .resolve("dataset-f8dba866-9d1b-492e-b76c-f4a78ac3958c/")
        .let { bucket ->
            Dataset(
                label = "7k issues JSW 7.2.0",
                database = MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = ofMinutes(5)
                    )
                ),
                jiraHomeSource = JiraHomePackage(
                    HttpDatasetPackage(
                        uri = bucket.resolve("jirahome.tar.bz2"),
                        downloadTimeout = ofMinutes(5)
                    )
                )
            )
        }
        .let { dataset -> DockerJiraFormula(Jperf424WorkaroundJswDistro("7.2.0"), dataset) }

    /**
     * We don't have a quick Jira formula with a ton of users.
     * Blocked by [JPERF-1089](https://ecosystem.atlassian.net/browse/JPERF-1089).
     */
     val MANY_USERS_JIRA = SMALL_JIRA
}
