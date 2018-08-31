package com.atlassian.performance.tools.virtualusers

import java.time.Duration

data class VirtualUserLoad(
    val virtualUsers: Int = 10,
    val hold: Duration = Duration.ZERO,
    val ramp: Duration = Duration.ofSeconds(15),
    val flat: Duration = Duration.ofMinutes(5)
) {
    val total: Duration = hold + ramp + flat
    val rampInterval: Duration = ramp.dividedBy(virtualUsers.toLong())

    fun subdivide(
        parts: Int
    ): List<VirtualUserLoad> {
        if (parts > virtualUsers) {
            throw Exception("$virtualUsers virtual users are not enough to subdivide into $parts parts")
        }
        val subVus = virtualUsers / parts
        val subRamp = ramp.dividedBy(parts.toLong())
        return (1L..parts).map { part ->
            VirtualUserLoad(
                virtualUsers = subVus,
                hold = hold + subRamp.multipliedBy(part - 1),
                ramp = subRamp,
                flat = flat + subRamp.multipliedBy(parts - part)
            )
        }
    }
}