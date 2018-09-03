package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.virtualusers.LoadTest

/**
 * A [load test](https://en.wikipedia.org/wiki/Load_testing).
 */
@Deprecated(message = "Do not use. Use com.atlassian.performance.tools.virtualusers.api.main method instead.")
class BasicTest(
    private val options: VirtualUserOptions
) {
    fun run() {
        LoadTest(options).run()
    }
}