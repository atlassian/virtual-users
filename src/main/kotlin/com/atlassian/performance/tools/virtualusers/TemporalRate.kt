package com.atlassian.performance.tools.virtualusers

import java.time.Duration
import kotlin.math.roundToLong

/**
 * https://en.wikipedia.org/wiki/Rate_(mathematics)#Temporal_rates
 */
class TemporalRate(
    val change: Double,
    val time: Duration
) : Comparable<TemporalRate> {

    override fun compareTo(other: TemporalRate): Int {
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

    operator fun plus(
        other: TemporalRate
    ): TemporalRate {
        val (normalizedThis, normalizedOther) = normalizeTimes(other)
        return TemporalRate(
            normalizedThis.change + normalizedOther.change,
            normalizedThis.time
        )
    }

    operator fun minus(
        other: TemporalRate
    ): TemporalRate {
        val (normalizedThis, normalizedOther) = normalizeTimes(other)
        return TemporalRate(
            normalizedThis.change - normalizedOther.change,
            normalizedThis.time
        )
    }

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

    override fun toString(): String = "$change over $time"

    companion object {
        val ZERO = TemporalRate(0.0, Duration.ofSeconds(1))
    }
}
