package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.jiraactions.api.memories.IssueKeyMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveIssueKeyMemory
import com.atlassian.performance.tools.virtualusers.ExploratoryVirtualUser
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.config.LoadThreadContainerDefaults
import com.atlassian.performance.tools.virtualusers.diagnostics.DisabledDiagnostics
import okhttp3.*
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.json.spi.JsonProvider

class HttpLoadProcess : LoadProcess {

    override fun setUp(container: LoadProcessContainer): LoadThreadFactory {
        return HttpLoadThreadFactory()
    }
}

private class HttpLoadThreadFactory : LoadThreadFactory {

    override fun fireUp(container: LoadThreadContainer): LoadThread {
        container.
        val myContainer = LoadThreadContainer.Builder(container)
            .actionMeter(Supplier {
                val actionOutput = container.threadResult().writeActionMetrics()
                container.addCloseable(actionOutput)
                ActionMeter.Builder(AppendableActionMetricOutput(actionOutput))
                    .virtualUser(container.uuid)
                    .build()
            })
            .build()
        val target = container.loadProcessContainer().options().target
        val http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain
                    .request()
                    .newBuilder()
                    .header("Authorization", Credentials.basic(target.userName, target.password))
                    .build()
                    .let { chain.proceed(it) }
            }
            .build()
        val baseUri = target.webApplication
        val seededRandom = SeededRandom(container.random().nextLong())
        val issueKeyMemory = AdaptiveIssueKeyMemory(seededRandom)
        val actions = listOf(
            RestSearch(baseUri, http, actionMeter, issueKeyMemory)
        )
        val looper = ExploratoryVirtualUser(
            container.singleThreadLoad(),
            container.taskMeter(),
            actions,
            DisabledDiagnostics()
        )
        return object : LoadThread {
            override fun generateLoad(
                stop: AtomicBoolean
            ) {
                looper.hold()
                looper.applyLoad(stop)
            }
        }
    }
}


class RestSearch(
    private val base: URI,
    private val http: OkHttpClient,
    private val meter: ActionMeter,
    private val memory: IssueKeyMemory
) : Action {

    companion object {
        val REST_SEARCH = ActionType("POST search") {}
        private val mediaTypeJson = MediaType.parse("application/json; charset=utf-8")
    }

    private val json = JsonProvider.provider()
    private val requestJson = json.createObjectBuilder()
        .add("jql", "resolved is empty")
        .add("startAt", 0)
        .add("maxResults", 15)
        .add(
            "fields",
            json.createArrayBuilder()
                .add("summary")
                .add("status")
                .add("assignee")
        )
        .build()

    override fun run() {
        meter.measure(REST_SEARCH) {
            val request = Request.Builder()
                .post(RequestBody.create(mediaTypeJson, requestJson.toString()))
                .url(base.resolve("rest/api/2/search").toURL())
                .build()
            val response = http.newCall(request).execute()
            if (response.isSuccessful.not()) {
                throw Exception("Response failed: $response")
            }
            val responseJson = json.createReader(response.body()!!.byteStream()).readObject()
            if (responseJson.getInt("total") <= 0) {
                throw Exception("Expected more than zero issues, got $responseJson")
            }
            val issueKeys = responseJson
                .getJsonArray("issues")
                .map { it.asJsonObject().getString("key") }
            memory.remember(issueKeys)
        }
    }
}
