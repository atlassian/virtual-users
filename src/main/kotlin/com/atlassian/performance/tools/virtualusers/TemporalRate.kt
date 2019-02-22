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
        val shorterTime = minOf(this.time, other.time)
        return compareValues(
            this.scaleTime(shorterTime).change,
            other.scaleTime(shorterTime).change
        )
    }

    operator fun plus(
        other: TemporalRate
    ): TemporalRate {
        if (this.time == other.time) {
            return TemporalRate(
                change = this.change + other.change,
                time = this.time
            )
        } else {
            throw Exception("We're not able to translate different time units yet")
        }
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
