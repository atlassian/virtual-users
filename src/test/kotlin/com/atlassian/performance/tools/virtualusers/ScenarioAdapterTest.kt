package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveUserMemory
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.api.diagnostics.DriverMock
import org.junit.Assert
import org.junit.Test
import java.net.URI
import java.util.*

class ScenarioAdapterTest {
    private val scenarioAdapter = ScenarioAdapter(JiraSoftwareScenario())
    private val webJira = WebJira(
            driver = DriverMock(),
            base = URI("http://localhost"),
            adminPassword = "admin"
    )
    private val meter = ActionMeter(
            virtualUser = UUID.randomUUID()
    )
    private val userMemory = AdaptiveUserMemory(SeededRandom())

    @Test
    fun shouldReturnLogInAction() {
        val logInAction = scenarioAdapter.getLogInAction(webJira, meter, userMemory)

        Assert.assertNotEquals(logInAction, null)
    }

    @Test
    fun shouldReturnSetupAction() {
        val setupAction = scenarioAdapter.getSetupAction(webJira, meter)

        Assert.assertNotEquals(setupAction, null)
    }

}