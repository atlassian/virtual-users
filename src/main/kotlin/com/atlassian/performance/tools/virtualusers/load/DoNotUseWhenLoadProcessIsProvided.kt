package com.atlassian.performance.tools.virtualusers.load

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

class DoNotUseWhenLoadProcessIsProvided : Scenario {

    init {
        throw Exception("This should not be created, use the LoadProcess")
    }

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): MutableList<Action> {
        throw Exception("This should not be used, use the LoadProcess")
    }
}
