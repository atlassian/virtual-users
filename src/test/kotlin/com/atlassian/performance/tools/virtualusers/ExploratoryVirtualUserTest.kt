package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.measure.ApplicationNode
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.measureTimeMillis

class ExploratoryVirtualUserTest {

    @Test
    fun shouldRespectMaxLoad() {
        val action = QuickServer()
        val maxLoad = TemporalRate(40.0, ofMinutes(1))
        val virtualUser = ExploratoryVirtualUser(
            node = StaticApplicationNode(),
            nodeCounter = JiraNodeCounter(),
            actions = listOf(action),
            setUpAction = NoOp(),
            logInAction = NoOp(),
            maxLoad = maxLoad,
            diagnostics = DisabledDiagnostics()
        )
        Timer(true).schedule(ofSeconds(5).toMillis()) {
            ExploratoryVirtualUser.shutdown.set(true)
        }

        val totalDuration = ofMillis(measureTimeMillis {
            virtualUser.applyLoad()
        })

        val actualLoad = TemporalRate(action.requestsServed.toDouble(), totalDuration)
        val hopefullyMinLoad = TemporalRate(30.0, ofMinutes(1))
        assertThat(actualLoad).isBetween(hopefullyMinLoad, maxLoad)
    }

    private class QuickServer : Action {

        var requestsServed = 0L

        override fun run() {
            Thread.sleep(10)
            requestsServed++
        }
    }

    private class NoOp : Action {
        override fun run() = Unit
    }

    private class DisabledDiagnostics : Diagnostics {
        override fun diagnose(exception: Exception): Unit = Unit
    }

    private class StaticApplicationNode : ApplicationNode {
        override fun identify(): String = "test-node"
    }
}
