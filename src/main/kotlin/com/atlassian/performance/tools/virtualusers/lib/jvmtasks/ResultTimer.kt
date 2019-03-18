package com.atlassian.performance.tools.virtualusers.lib.jvmtasks

import java.time.Duration
import java.time.Instant.now

object ResultTimer {

    fun <T> timeWithResult(
        task: () -> T
    ): TimedResult<T> {
        val start = now()
        val result = task()
        val duration = Duration.between(start, now())
        return TimedResult(
            result,
            duration
        )
    }
}

class TimedResult<T>(
    val result: T,
    val duration: Duration
)
