package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.ThrowawayActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.diagnostics.DisabledDiagnostics
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class ScenarioThreadTest {

    @Test
    @Ignore("TDD: red")
    fun shouldWaitBeforeLogin() {
        // given
        val expectedHold = Duration.ofMillis(500)
        val actionLoop = ThrottlingActionLoop(
            maxLoad = TemporalRate(40.0, Duration.ofMillis(1)),
            taskMeter = ActionMeter.Builder(ThrowawayActionMetricOutput()).build(),
            actions = emptyList(),
            diagnostics = DisabledDiagnostics()
        )
        val load = VirtualUserLoad.Builder().hold(expectedHold).build()
        var rightBeforeLogin: Instant? = null
        val scenarioThread = ScenarioThread(
            load = load,
            looper = actionLoop,
            userLogin = object : Action {
                override fun run() {
                    rightBeforeLogin = Instant.now()
                }
            },
            countNode = object : Action {
                override fun run() {}
            }
        )
        // when
        val beforeLoad = Instant.now()
        scenarioThread.generateLoad(AtomicBoolean(false))
        // then
        val actualHold = Duration.between(beforeLoad, rightBeforeLogin)
        assertThat(actualHold).isGreaterThanOrEqualTo(expectedHold)
    }
}
