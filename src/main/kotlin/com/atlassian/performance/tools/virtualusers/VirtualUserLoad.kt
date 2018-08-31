package com.atlassian.performance.tools.virtualusers

import java.time.Duration

data class VirtualUserLoad(
    val virtualUsers: Int,
    val hold: Duration,
    val ramp: Duration,
    val load: Duration
)