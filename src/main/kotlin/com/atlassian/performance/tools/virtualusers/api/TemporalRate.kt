package com.atlassian.performance.tools.virtualusers.api

import java.time.Duration
import kotlin.math.roundToLong

/**
 * Rate of [change] over [time]. For example, HTTP throughput, VU action arrival rate, speed, frequency.
 *
 * See [more examples on Wikipedia](https://en.wikipedia.org/wiki/Rate_(mathematics)#Temporal_rates).
 *
 * @since 3.5.0
 */
class TemporalRate(
    val change: Double,
    val time: Duration
) : Comparable<TemporalRate> {

    fun scaleChange(
        newChange: Double
    ): TemporalRate {
        if (change == newChange) {
            return this
        }
        val factor = newChange / change
        return TemporalRate(
            change = newChange,
            time = Duration.ofNanos((time.toNanos() * factor).roundToLong())
        )
    }

    fun scaleTime(
        newTime: Duration
    ): TemporalRate {
        if (time == newTime) {
            return this
        }
        val factor = newTime.toMillis().toDouble() / time.toMillis().toDouble()
        return TemporalRate(
            change = change * factor,
            time = newTime
        )
    }

    operator fun times(
        factor: Double
    ): TemporalRate = TemporalRate(
        change = change * factor,
        time = time
    )

    operator fun div(
        divisor: Int
    ): TemporalRate = TemporalRate(
        change = change / divisor,
        time = time
    )

    override fun compareTo(
        other: TemporalRate
    ): Int {
        val (normalizedThis, normalizedOther) = normalizeTimes(other)
        return compareValues(
            normalizedThis.change,
            normalizedOther.change
        )
    }

    private fun normalizeTimes(
        other: TemporalRate
    ): Pair<TemporalRate, TemporalRate> {
        val shorterTime = minOf(this.time, other.time)
        return Pair(
            this.scaleTime(shorterTime),
            other.scaleTime(shorterTime)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemporalRate

        val (normalizedThis, normalizedOther) = normalizeTimes(other)

        if (normalizedThis.change != normalizedOther.change) return false
        if (normalizedThis.time != normalizedOther.time) return false

        return true
    }

    override fun hashCode(): Int {
        val standardized = scaleTime(Duration.ofMillis(1))
        var result = standardized.change.hashCode()
        result = 31 * result + standardized.time.hashCode()
        return result
    }

    override fun toString(): String = "$change over $time"
}
