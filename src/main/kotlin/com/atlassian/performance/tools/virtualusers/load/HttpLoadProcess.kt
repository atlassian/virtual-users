package com.atlassian.performance.tools.virtualusers.load

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.IssueKeyMemory
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveIssueKeyMemory
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.api.load.LoadProcess
import com.atlassian.performance.tools.virtualusers.api.load.LoadThread
import com.atlassian.performance.tools.virtualusers.api.load.LoadThreadFactory
import com.atlassian.performance.tools.virtualusers.api.load.ThrottlingActionLoop
import com.atlassian.performance.tools.virtualusers.diagnostics.DisabledDiagnostics
import okhttp3.*
import java.net.URI
import javax.json.spi.JsonProvider

class HttpLoadProcess : LoadProcess {

    override fun prepareFactory(container: LoadProcessContainer): LoadThreadFactory {
        return HttpLoadThreadFactory()
    }
}

private class HttpLoadThreadFactory : LoadThreadFactory {

    override fun prepareThread(container: LoadThreadContainer): LoadThread {
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
            RestSearch(baseUri, http, container.actionMeter(), issueKeyMemory)
        )
        return ThrottlingActionLoop(
            container.singleThreadLoad().maxOverallLoad,
            container.taskMeter(),
            actions,
            DisabledDiagnostics()
        )
    }
}

private class RestSearch(
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
            val responseJson = response.body()!!.use {
                json.createReader(it.byteStream()).readObject()
            }
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
