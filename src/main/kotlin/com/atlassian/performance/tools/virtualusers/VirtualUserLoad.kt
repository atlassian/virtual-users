package com.atlassian.performance.tools.virtualusers

import java.time.Duration

data class VirtualUserLoad(
    val virtualUsers: Int = 10,
    val hold: Duration = Duration.ofSeconds(0),
    val ramp: Duration = Duration.ofSeconds(0),
    val load: Duration = Duration.ofMinutes(10)
) {
    val test: Duration = hold + ramp + load
}