package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

internal class NoopScenario : Scenario {
    private val noopAction = object : Action {
        override fun run() {
        }
    }

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return listOf(noopAction)
    }

    override fun getSetupAction(jira: WebJira, meter: ActionMeter): Action {
        return noopAction
    }

    override fun getLogInAction(jira: WebJira, meter: ActionMeter, userMemory: UserMemory): Action {
        return noopAction
    }
}