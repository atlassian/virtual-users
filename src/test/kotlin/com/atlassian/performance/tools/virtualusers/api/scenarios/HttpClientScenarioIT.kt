package com.atlassian.performance.tools.virtualusers.api.scenarios

import com.atlassian.performance.tools.io.api.directories
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.LoadTest
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.Future

class HttpClientScenarioIT {
    private val workspace = Paths.get("test-results")

    @Before
    fun before() {
        workspace.toFile().deleteRecursively()
    }

    @After
    fun after() {
        workspace.toFile().deleteRecursively()
    }

    @Test
    fun shouldConsumeHttpClientBasedScenario() {
        // given
        val loadTest = LoadTest(
            VirtualUserOptions(
                target = VirtualUserTarget(
                    webApplication = URI("http://dummy.restapiexample.com/"),
                    userName = "admin",
                    password = "admin"
                ),
                behavior = VirtualUserBehavior.HttpClientVirtualUsersBuilder(
                    HttpClientScenarioExample::class.java
                ).load(
                    VirtualUserLoad.Builder()
                        .virtualUsers(3)
                        .ramp(Duration.ZERO)
                        .flat(Duration.ofSeconds(3))
                        .build()
                ).build()
            )
        )

        // when
        loadTest.run()

        // then
        val metrics = workspace.toFile().directories().flatMap {
            it.listFiles()!!.flatMap { actionMetrics ->
                actionMetrics.readLines()
            }
        }

        Assertions.assertThat(metrics.size).isGreaterThan(5)
        Assertions.assertThat(metrics).anyMatch { metric ->
            metric.contains(""""result":"OK"""")
        }
        Assertions.assertThat(metrics).noneMatch { metric ->
            metric.contains(""""result":"ERROR"""")
        }
    }

    class HttpClientScenarioExample : HttpClientScenario {
        override fun getActions(httpClient: Future<CloseableHttpClient>, jiraUri: URI, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
            return listOf(JqlSearchAction(httpClient, jiraUri, meter))
        }

        private class JqlSearchAction(
            private val httpClientFuture: Future<CloseableHttpClient>,
            jiraUri: URI,
            private val meter: ActionMeter
        ) : Action {
            private val httpGet = HttpGet(jiraUri.resolve("api/v1/employees"))
            private val actionType = ActionType("A REST call") { Unit }
            override fun run() {
                val httpClient = httpClientFuture.get()
                meter.measure(actionType) {
                    httpClient.execute(httpGet).use { response ->
                        if (response.statusLine.statusCode != 200) {
                            throw Exception("Failed to test Rest $httpGet : $response")
                        }
                    }
                }
            }
        }
    }
}
