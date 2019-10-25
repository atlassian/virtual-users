package com.atlassian.performance.tools.virtualusers.api.scenarios

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import org.apache.http.impl.client.CloseableHttpClient
import java.net.URI
import java.util.concurrent.Future

interface HttpClientScenario {
    fun getActions(httpClient: Future<CloseableHttpClient>, jiraUri: URI, seededRandom: SeededRandom, meter: ActionMeter): List<Action>
}
