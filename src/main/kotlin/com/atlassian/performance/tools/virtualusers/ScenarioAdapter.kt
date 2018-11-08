package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.action.LogInAction
import com.atlassian.performance.tools.jiraactions.api.action.SetUpAction
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario

/**
 * Compatibility layer to avoid MAJOR version bump, because of a transitive dependency bump.
 * Adapter avoids to require `jira-actions:3.x` at compile time. We can remove the class, as soon we stop
 * to support `jira-actions:2.x`.
 */
internal class ScenarioAdapter(
    private val scenario: Scenario
) {
    fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return scenario.getActions(jira, seededRandom, meter)
    }

    fun getLogInAction(jira: WebJira, meter: ActionMeter, userMemory: UserMemory): Action {
        return if (isScenario3Compatible()) {
            val getLogInActionMethod = scenario::class.java.getMethod("getLogInAction", WebJira::class.java, ActionMeter::class.java, UserMemory::class.java)
            getLogInActionMethod(scenario, jira, meter, userMemory) as Action
        } else {
            LogInAction(
                jira = jira,
                meter = meter,
                userMemory = userMemory
            )
        }
    }

    fun getSetupAction(jira: WebJira, meter: ActionMeter): Action {
        return if (isScenario3Compatible()) {
            val getSetupActionMethod = scenario::class.java.getMethod("getSetupAction", WebJira::class.java, ActionMeter::class.java)
            getSetupActionMethod(scenario, jira, meter) as Action
        } else {
            SetUpAction(
                jira = jira,
                meter = meter
            )
        }
    }

    private fun isScenario3Compatible(): Boolean {
        val methods = scenario::class
            .java
            .methods
            .map { it.name }

        return methods.contains("getSetupAction") && methods.contains("getLogInAction")
    }

}