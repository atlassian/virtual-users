package com.atlassian.performance.tools.virtualusers.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration.ofSeconds

class VirtualUserLoadTest {

    private val load = VirtualUserLoad.Builder()
        .virtualUsers(12)
        .hold(ofSeconds(10))
        .ramp(ofSeconds(16))
        .flat(ofSeconds(55))
        .maxOverallLoad(TemporalRate(19.20, ofSeconds(1)))
        .build()

    @Test
    fun shouldSpreadLoadOverFourNodes() {
        val slices = load.slice(4)

        assertThat(slices).containsExactly(
            VirtualUserLoad.Builder()
                .virtualUsers(3)
                .hold(ofSeconds(10))
                .ramp(ofSeconds(4))
                .flat(ofSeconds(67))
                .maxOverallLoad(TemporalRate(4.8, ofSeconds(1)))
                .build(),
            VirtualUserLoad.Builder()
                .virtualUsers(3)
                .hold(ofSeconds(14))
                .ramp(ofSeconds(4))
                .flat(ofSeconds(63))
                .maxOverallLoad(TemporalRate(4.8, ofSeconds(1)))
                .build(),
            VirtualUserLoad.Builder()
                .virtualUsers(3)
                .hold(ofSeconds(18))
                .ramp(ofSeconds(4))
                .flat(ofSeconds(59))
                .maxOverallLoad(TemporalRate(4.8, ofSeconds(1)))
                .build(),
            VirtualUserLoad.Builder()
                .virtualUsers(3)
                .hold(ofSeconds(22))
                .ramp(ofSeconds(4))
                .flat(ofSeconds(55))
                .maxOverallLoad(TemporalRate(4.8, ofSeconds(1)))
                .build()
        )
    }

    @Test
    fun shouldSpreadTheSameLoad() {
        val slices = load.slice(1)

        assertThat(slices).containsExactly(load)
    }

    @Test
    fun shouldSpreadWithEqualTotals() {
        val slices = load.slice(10)

        assertThat(slices.map { it.total }).containsOnly(load.total)
    }

    @Test
    fun shouldAvoidEmptyNodes() {
        val tooManySlices = 300

        val exception: Exception? = try {
            load.slice(tooManySlices)
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception).isNotNull()
        assertThat(exception).hasMessage("12 virtual users are not enough to slice into $tooManySlices")
    }
}
