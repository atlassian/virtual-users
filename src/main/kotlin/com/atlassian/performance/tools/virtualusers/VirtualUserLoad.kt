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

    @Deprecated(message = "For Seba compatibility :)", replaceWith = ReplaceWith("flat"))
    val load = flat

    @Deprecated(message = "For Seba compatibility :)", replaceWith = ReplaceWith("total"))
    val test: Duration = total
}