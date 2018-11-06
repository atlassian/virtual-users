package com.atlassian.performance.tools.virtualusers.api

import java.time.Duration

/**
 * Defines load for a test.
 *
 * [virtualUsers] number of virtual users used to generate load
 * [hold] initial duration when no load is generated
 * [ramp] duration when load is ramping up
 * [flat] duration when full load is generated
 */
class VirtualUserLoad(
    val virtualUsers: Int = 10,
    val hold: Duration = Duration.ZERO,
    val ramp: Duration = Duration.ofSeconds(15),
    val flat: Duration = Duration.ofMinutes(5)
) {
    val total: Duration = hold + ramp + flat
    val rampInterval: Duration = ramp.dividedBy(virtualUsers.toLong())
}