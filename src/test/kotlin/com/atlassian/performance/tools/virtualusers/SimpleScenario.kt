package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

class SimpleScenario : Scenario {

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return listOf(
            HardcodedViewIssueAction(jira, meter),
            FailingAction()
        )
    }
}

class HardcodedViewIssueAction(
    private val jira: WebJira,
    private val meter: ActionMeter
) : Action {

    private val viewIssueAction = ActionType("View Issue") { Unit }

    override fun run() {
        meter.measure(viewIssueAction) {
            jira.goToIssue("SAM-1").waitForSummary()
        }
    }
}

class FailingAction : Action {
    override fun run() {
        throw Exception("deliberate fail")
    }

}
