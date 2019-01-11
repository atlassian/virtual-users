package com.atlassian.performance.tools.virtualusers.api.browsers

import org.junit.Test
import java.lang.reflect.Constructor

class HeadlessChromeBrowserTest {

    @Test
    fun shouldCreateHeadlessChromeInstance() {
        val zeroArgConstructor: Constructor<HeadlessChromeBrowser> = HeadlessChromeBrowser::class.java.getConstructor()
        zeroArgConstructor.newInstance()
    }

    @Test
    fun shouldStartMultipleInParallel() {
        val browser = HeadlessChromeBrowser()

        (1..20)
            .toList()
            .parallelStream()
            .forEach { browser.start().use {} }
    }
}