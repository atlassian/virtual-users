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

     val MANY_USERS_JIRA = URI("https://s3-eu-central-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-dah44h6l1l8p/")
        .resolve("jsw-7.13.0-100k-users-sync/")
        .let { bucket ->
            Dataset(
                database = MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = ofMinutes(6)
                    )
                ),
                jiraHomeSource = JiraHomePackage(
                    HttpDatasetPackage(
                        uri = bucket.resolve("jirahome.tar.bz2"),
                        downloadTimeout = ofMinutes(6)
                    )
                ),
                label = "100k users"
            )
        }
        .let { dataset -> DockerJiraFormula(Jperf424WorkaroundJswDistro("7.13.0"), dataset) }
}
