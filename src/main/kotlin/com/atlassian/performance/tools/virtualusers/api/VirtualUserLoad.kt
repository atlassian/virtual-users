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
