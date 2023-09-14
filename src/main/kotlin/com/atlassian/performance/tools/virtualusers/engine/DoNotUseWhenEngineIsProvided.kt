package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

class DoNotUseWhenEngineIsProvided : Scenario {

    init {
        throw Exception("This should not be created, use the Engine")
    }

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): MutableList<Action> {
        throw Exception("This should not be used, use the Engine")
    }
}
