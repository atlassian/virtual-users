package com.atlassian.performance.tools.virtualusers.api.browsers

import org.junit.Test

class HeadlessChromeBrowserIT {

    @Test
    fun shouldStartMultipleInParallel() {
        val browser = HeadlessChromeBrowser()

        (1..20)
            .toList()
            .parallelStream()
            .forEach { browser.start().use {} }
    }
}
