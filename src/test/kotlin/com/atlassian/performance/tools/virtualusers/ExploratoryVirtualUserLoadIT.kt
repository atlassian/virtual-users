package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.ThrowawayActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.*
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.diagnostics.DisabledDiagnostics
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Test
import java.time.Duration
import java.time.Duration.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule
import kotlin.math.exp
import kotlin.system.measureTimeMillis

/**
 * Tests integration of [VirtualUserLoad.maxOverallLoad] and [Action]s with various [Thread.sleep] timings.
 */
class ExploratoryVirtualUserLoadIT {

    @Test
    fun shouldRespectMaxLoad() {
        val maxLoad = TemporalRate(40.0, ofMinutes(1))
        val server = UnreliableServer(QuickServer())
        val virtualUser = prepareVu(listOf(server), maxLoad)

        val totalDuration = applyLoad(virtualUser, ofSeconds(5))

        val actualLoad = TemporalRate(server.getRequestsServed().toDouble(), totalDuration)
        val unambitiousMinLoad = maxLoad * 0.75
        assertLoadInRange(actualLoad, unambitiousMinLoad, maxLoad)
    }

    @Test
    fun shouldBeCloseToMaxLoadDespiteSlowStart() {
        val maxLoad = TemporalRate(80.0, ofMinutes(1))
        val server = SlowlyWarmingServer(ofMillis(2500))
        val virtualUser = prepareVu(listOf(server), maxLoad)

        val totalDuration = applyLoad(virtualUser, ofSeconds(5))

        val actualLoad = TemporalRate(server.getRequestsServed().toDouble(), totalDuration)
        val closeToMaxLoad = maxLoad * 0.98
        assertLoadInRange(actualLoad, closeToMaxLoad, maxLoad)
    }

    // TODO this belongs to the ScenarioToEngineAdapter tests?
    @Test
    fun shouldNotRetryOnLoginAction() {
//        val server = QuickServer()
//        val logInAction = object : Action {
//            override fun run() {
//                throw Exception("Failed login attempt.")
//            }
//        }
//        val virtualUser = prepareVu(
//            actions = listOf(server),
//            maxLoad = TemporalRate(1_000_000.00, ofSeconds(1)),
//            logInAction = logInAction
//        )
//
//        val done = AutoCloseableExecutorService(Executors.newSingleThreadExecutor()).use { executorService ->
//            val applyLoadFuture = executorService.submit { virtualUser.applyLoad(AtomicBoolean(true)) }
//            Thread.sleep(1000)
//            return@use applyLoadFuture.isDone
//        }
//
//        assertThat(done).isTrue()
    }

    @Test
    fun shouldBeGoodEnoughDespiteUnevenLatencies() {
        val server = ParetoServer(
            eightyPercent = ofMillis(20),
            twentyPercent = ofMillis(300)
        )
        val maxLoad = TemporalRate(1.0, server.expectedMeanLatency)
        val virtualUser = prepareVu(listOf(server), maxLoad)

        val totalDuration = applyLoad(virtualUser, ofSeconds(10))

        val requestsServed = server.requestsServed
        assertThat(requestsServed).isGreaterThan(100)
        assertThat(server.eightiesServed.toDouble() / requestsServed).isCloseTo(0.80, offset(0.01))
        assertThat(server.twentiesServed.toDouble() / requestsServed).isCloseTo(0.20, offset(0.01))
        val actualLoad = TemporalRate(requestsServed.toDouble(), totalDuration)
        val goodEnoughMinLoad = maxLoad * 0.90
        assertLoadInRange(actualLoad, goodEnoughMinLoad, maxLoad)
    }

    private fun prepareVu(
        actions: List<Action>,
        maxLoad: TemporalRate
    ): ExploratoryVirtualUser = ExploratoryVirtualUser(
        actions = actions,
        diagnostics = DisabledDiagnostics(),
        taskMeter = mockMeter(),
        load = VirtualUserLoad.Builder()
            .maxOverallLoad(maxLoad)
            .build()
    )

    private fun applyLoad(
        virtualUser: ExploratoryVirtualUser,
        duration: Duration
    ): Duration {
        val done = AtomicBoolean(false)
        Timer(true).schedule(duration.toMillis()) {
            done.set(true)
        }
        return ofMillis(measureTimeMillis {
            virtualUser.applyLoad(done)
        })
    }

    private fun assertLoadInRange(
        actualLoad: TemporalRate,
        minLoad: TemporalRate,
        maxLoad: TemporalRate
    ) {
        val commonTimeUnit = ofHours(1)
        val readableActual = actualLoad.scaleTime(commonTimeUnit)
        val readableMin = minLoad.scaleTime(commonTimeUnit)
        val readableMax = maxLoad.scaleTime(commonTimeUnit)
        assertThat(readableActual).isBetween(readableMin, readableMax)
        println("Actual load [$readableActual] is good, because it fits between [$readableMin] and [$readableMax]")
    }

    private class UnreliableServer(private val server: Server) : Server {
        private var requestsServed = 0

        override fun getRequestsServed(): Int {
            return requestsServed
        }

        override fun run() {
            requestsServed++
            if (requestsServed.rem(2) == 0) {
                throw Exception()
            } else {
                server.run()
            }
        }
    }

    private class QuickServer : Server {
        private var requestsServed = 0

        override fun getRequestsServed(): Int {
            return requestsServed
        }

        override fun run() {
            Thread.sleep(10)
            requestsServed++
        }
    }

    private class SlowlyWarmingServer(
        private val slowStart: Duration
    ) : Server {
        private var requestsServed = 0

        override fun getRequestsServed(): Int {
            return requestsServed
        }

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

    private interface Server : Action {
        fun getRequestsServed(): Int
    }

    private class ParetoServer(
        private val eightyPercent: Duration,
        private val twentyPercent: Duration
    ) : Action {

        /**
         * With sample size as small as 100 requests, the resulting distribution is quite unstable.
         * E.g. a seed of `123123123` will generate a 79.4%/20.6% distro.
         * E.g. a seed of `123456789` will generate a 76.2%/23,8% distro.
         */
        private val random = Random(123123123)
        val expectedMeanLatency: Duration = (eightyPercent * 80 + twentyPercent * 20) / 100

        var requestsServed = 0
            private set
        var twentiesServed = 0
            private set
        var eightiesServed = 0
            private set

        override fun run() {
            if (random.nextDouble() < 0.80) {
                Thread.sleep(eightyPercent.toMillis())
                eightiesServed++
            } else {
                Thread.sleep(twentyPercent.toMillis())
                twentiesServed++
            }
            requestsServed++
        }

        private operator fun Duration.times(factor: Long): Duration = multipliedBy(factor)
        private operator fun Duration.div(divisor: Long) = dividedBy(divisor)
    }

    private fun mockMeter() = ActionMeter.Builder(ThrowawayActionMetricOutput()).build()

    class AutoCloseableExecutorService(executorService: ExecutorService) : ExecutorService by executorService,
        AutoCloseable {
        override fun close() {
            this.shutdownNow()
        }
    }
}
