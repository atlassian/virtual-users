package com.atlassian.performance.tools.virtualusers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration.*

class TemporalRateTest {

    @Test
    fun shouldScaleDown() {
        val original = TemporalRate(240.0, ofMinutes(1))

        val scaled = original.scaleTime(ofSeconds(1))

        assertThat(scaled.change).isEqualTo(4.0)
        assertThat(scaled.time).isEqualTo(ofSeconds(1))
    }

    @Test
    fun shouldScaleUp() {
        val original = TemporalRate(2.0, ofMinutes(1))

        val scaled = original.scaleTime(ofHours(1))

        assertThat(scaled.change).isEqualTo(120.0)
        assertThat(scaled.time).isEqualTo(ofHours(1))
    }

    @Test
    fun shouldScaleWithComplexTimeUnit() {
        val original = TemporalRate(100.0, ofSeconds(1))

        val scaled = original.scaleTime(ofSeconds(3))

        assertThat(scaled.change).isEqualTo(300.0)
        assertThat(scaled.time).isEqualTo(ofSeconds(3))
    }

    @Test
    fun shouldAdd() {
        val alpha = TemporalRate(3.0, ofSeconds(1))
        val beta = TemporalRate(5.0, ofSeconds(1))

        val sum = alpha + beta

        assertThat(sum.change).isEqualTo(8.0)
        assertThat(sum.time).isEqualTo(ofSeconds(1))
    }

    @Test
    fun shouldCompare() {
        val recordStarcraftActionsPerMinute = TemporalRate(818.0, ofMinutes(1))
        val monitorRefreshRate = TemporalRate(60.0, ofSeconds(1))

        assertThat(recordStarcraftActionsPerMinute).isLessThan(monitorRefreshRate)
        assertThat(monitorRefreshRate).isGreaterThan(recordStarcraftActionsPerMinute)
    }
}