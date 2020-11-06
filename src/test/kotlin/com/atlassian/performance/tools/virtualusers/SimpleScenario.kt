package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

class SimpleScenario : Scenario {

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return listOf(SeeSystemInfo(jira, meter))
    }
}

class SeeSystemInfo(
    private val jira: WebJira,
    private val meter: ActionMeter
) : Action {

    private val systemInfo = ActionType("See System Info") { }

    override fun run() {
        meter.measure(systemInfo) {
            jira.goToSystemInfo()
        }
    }
}
