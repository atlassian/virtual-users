package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import com.atlassian.performance.tools.virtualusers.measure.ApplicationNode
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Duration.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.exp
import kotlin.system.measureTimeMillis

class ExploratoryVirtualUserTest {

    @Test
    fun shouldRespectMaxLoad() {
        val maxLoad = TemporalRate(40.0, ofMinutes(1))
        val server = QuickServer()
        val virtualUser = prepareVu(listOf(server), maxLoad)

        val totalDuration = applyLoad(virtualUser)

        val actualLoad = TemporalRate(server.requestsServed.toDouble(), totalDuration)
        val hopefullyMinLoad = TemporalRate(30.0, ofMinutes(1))
        assertLoadInRange(actualLoad, hopefullyMinLoad, maxLoad)
    }

    @Test
    fun shouldBeCloseToMaxLoadDespiteSlowStart() {
        val maxLoad = TemporalRate(80.0, ofMinutes(1))
        val server = SlowlyWarmingServer(ofMillis(2500))
        val virtualUser = prepareVu(listOf(server), maxLoad)

        val totalDuration = applyLoad(virtualUser)

        val actualLoad = TemporalRate(server.requestsServed.toDouble(), totalDuration)
        val closeToMaxLoad = TemporalRate(70.0, ofMinutes(1))
        assertLoadInRange(actualLoad, closeToMaxLoad, maxLoad)
    }

    private fun prepareVu(
        actions: List<Action>,
        maxLoad: TemporalRate
    ): ExploratoryVirtualUser = ExploratoryVirtualUser(
        node = StaticApplicationNode(),
        nodeCounter = JiraNodeCounter(),
        actions = actions,
        setUpAction = NoOp(),
        logInAction = NoOp(),
        maxLoad = maxLoad,
        diagnostics = DisabledDiagnostics()
    )

    private fun applyLoad(
        virtualUser: ExploratoryVirtualUser
    ): Duration {
        Timer(true).schedule(ofSeconds(5).toMillis()) {
            ExploratoryVirtualUser.shutdown.set(true)
        }
        return ofMillis(measureTimeMillis {
            virtualUser.applyLoad()
        })
    }

    private fun assertLoadInRange(
        actualLoad: TemporalRate,
        minLoad: TemporalRate,
        maxLoad: TemporalRate
    ) {
        val commonTimeUnit = Duration.ofHours(1)
        val readableActual = actualLoad.scaleTime(commonTimeUnit)
        val readableMin = minLoad.scaleTime(commonTimeUnit)
        val readableMax = maxLoad.scaleTime(commonTimeUnit)
        assertThat(readableActual).isBetween(readableMin, readableMax)
        println("Actual load [$readableActual] is good, because it fits between [$readableMin] and [$readableMax]")
    }

    private class QuickServer : Action {

        var requestsServed = 0
            private set

        override fun run() {
            Thread.sleep(10)
            requestsServed++
        }
    }

    private class SlowlyWarmingServer(
        private val slowStart: Duration
    ) : Action {

        var requestsServed = 0
            private set

        override fun run() {
            Thread.sleep(decayExponentially().toMillis())
            requestsServed++
        }

        private fun decayExponentially(): Duration {
            val decay = exp(-requestsServed.toDouble())
            val decayedNanos = slowStart.toNanos() * decay
            return ofNanos(decayedNanos.toLong())
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
