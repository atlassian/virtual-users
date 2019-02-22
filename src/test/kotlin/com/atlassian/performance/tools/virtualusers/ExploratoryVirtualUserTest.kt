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
    fun shouldBeSlowLikeHumans() {
        val action = QuickServer()
        val virtualUser = ExploratoryVirtualUser(
            node = StaticApplicationNode(),
            nodeCounter = JiraNodeCounter(),
            actions = listOf(action),
            setUpAction = NoOp(),
            logInAction = NoOp(),
            diagnostics = DisabledDiagnostics()
        )
        Timer(true).schedule(ofSeconds(5).toMillis()) {
            ExploratoryVirtualUser.shutdown.set(true)
        }

        val totalDuration = ofMillis(measureTimeMillis {
            virtualUser.applyLoad()
        })

        val throughput = TemporalRate(action.requestsServed.toDouble(), totalDuration)
        val recordHumanApm = TemporalRate(818.0, ofMinutes(1)) // https://en.wikipedia.org/wiki/Actions_per_minute
        assertThat(throughput).isLessThan(recordHumanApm)
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
