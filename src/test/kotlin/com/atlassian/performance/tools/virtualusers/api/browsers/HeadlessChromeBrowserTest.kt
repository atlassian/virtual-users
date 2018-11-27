package com.atlassian.performance.tools.virtualusers.api.browsers

import org.junit.Test
import java.lang.reflect.Constructor

class HeadlessChromeBrowserTest {

    @Test
    fun shouldCreateHeadlessChromeInstance() {
        val zeroArgConstructor: Constructor<HeadlessChromeBrowser> = HeadlessChromeBrowser::class.java.getConstructor()
        zeroArgConstructor.newInstance()
    }
}