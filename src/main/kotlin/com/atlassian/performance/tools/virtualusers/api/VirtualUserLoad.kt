package com.atlassian.performance.tools.virtualusers.api

import java.time.Duration

/**
 * Specifies the intended load VUs should try to achieve.
 *
 * [virtualUsers] number of virtual users used to generate load
 * [hold] initial duration when no load is generated
 * [ramp] duration when load is ramping up
 * [flat] duration when full load is generated
 * [maxOverallLoad] maximum action rate across all VUs throughout the entire duration
 */
class VirtualUserLoad private constructor(
    val virtualUsers: Int,
    val hold: Duration,
    val ramp: Duration,
    val flat: Duration,
    val maxOverallLoad: TemporalRate
) {
    val total: Duration = hold + ramp + flat
    val rampInterval: Duration = ramp.dividedBy(virtualUsers.toLong())

    @Deprecated("Use VirtualUserLoad.Builder instead")
    constructor(
        virtualUsers: Int = 10,
        hold: Duration = Duration.ZERO,
        ramp: Duration = Duration.ofSeconds(15),
        flat: Duration = Duration.ofMinutes(5)
    ) : this(
        virtualUsers,
        hold,
        ramp,
        flat,
        maxOverallLoad = TemporalRate(10.0, Duration.ofSeconds(1))
    )

    /**
     * Slice this load into multiple smaller loads. When stacked together, the slices exactly fit the original.
     *
     * @param [slices] Controls the number of output slices.
     * @return Slices, which compose back to the original load, if launched in order, in parallel and at the same time.
     * @since 3.5.0
     */
    fun slice(
        slices: Int
    ): List<VirtualUserLoad> {
        if (slices > virtualUsers) {
            throw Exception("$virtualUsers virtual users are not enough to slice into $slices")
        }
        val vusPerSlice = virtualUsers / slices
        val rampPerSlice = ramp.dividedBy(slices.toLong())
        return (0L until slices).map { slice ->
            val slicesAbove = slices - slice - 1
            VirtualUserLoad.Builder()
                .virtualUsers(vusPerSlice)
                .hold(hold + rampPerSlice * slice)
                .ramp(rampPerSlice)
                .flat(flat + rampPerSlice * slicesAbove)
                .maxOverallLoad(maxOverallLoad / slices)
                .build()
        }
    }

    private operator fun Duration.times(
        factor: Long
    ) = multipliedBy(factor)


    override fun toString(): String {
        return "VirtualUserLoad(" +
            "virtualUsers=$virtualUsers, " +
            "hold=$hold, " +
            "ramp=$ramp, " +
            "flat=$flat, " +
            "maxOverallLoad=$maxOverallLoad" +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualUserLoad

        if (virtualUsers != other.virtualUsers) return false
        if (hold != other.hold) return false
        if (ramp != other.ramp) return false
        if (flat != other.flat) return false
        if (maxOverallLoad != other.maxOverallLoad) return false

        return true
    }

    override fun hashCode(): Int {
        var result = virtualUsers
        result = 31 * result + hold.hashCode()
        result = 31 * result + ramp.hashCode()
        result = 31 * result + flat.hashCode()
        result = 31 * result + maxOverallLoad.hashCode()
        return result
    }

    class Builder {

        private var virtualUsers: Int = 10
        private var hold: Duration = Duration.ZERO
        private var ramp: Duration = Duration.ofSeconds(15)
        private var flat: Duration = Duration.ofMinutes(5)
        private var maxOverallLoad: TemporalRate = TemporalRate(10.0, Duration.ofSeconds(1))

        fun virtualUsers(virtualUsers: Int) = apply { this.virtualUsers = virtualUsers }
        fun hold(hold: Duration) = apply { this.hold = hold }
        fun ramp(ramp: Duration) = apply { this.ramp = ramp }
        fun flat(flat: Duration) = apply { this.flat = flat }
        fun maxOverallLoad(maxOverallLoad: TemporalRate) = apply { this.maxOverallLoad = maxOverallLoad }

        fun build(): VirtualUserLoad = VirtualUserLoad(
            virtualUsers,
            hold,
            ramp,
            flat,
            maxOverallLoad
        )
    }
}
