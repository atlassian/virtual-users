package com.atlassian.performance.tools.virtualusers.scenarios

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.HttpClientWebDriver
import com.atlassian.performance.tools.virtualusers.api.scenarios.HttpClientScenario

internal class HttpClientScenarioAdapter : Scenario {
    internal companion object {
        @Volatile
        internal var scenarioClass: Class<out HttpClientScenario>? = null
    }

    private val scenario: HttpClientScenario = scenarioClass!!.getConstructor().newInstance()
    override fun getSetupAction(jira: WebJira, meter: ActionMeter): Action {
        return NoopAction()
    }

    override fun getLogInAction(jira: WebJira, meter: ActionMeter, userMemory: UserMemory): Action {
        val user = userMemory.recall()!!
        (jira.driver as HttpClientWebDriver).initHttpClient(user.name, user.password)
        return NoopAction()
    }

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return scenario.getActions((jira.driver as HttpClientWebDriver).getHttpClientFuture(), jira.base, seededRandom, meter)
    }

    private class NoopAction : Action {
        override fun run() {}
    }

}
