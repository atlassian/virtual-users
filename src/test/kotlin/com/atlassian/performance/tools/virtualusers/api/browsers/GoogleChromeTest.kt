package com.atlassian.performance.tools.virtualusers.api.browsers

import org.junit.Test
import java.lang.reflect.Constructor

class GoogleChromeTest {

    @Test
    fun shouldCreateChromeInstance() {
        val zeroArgConstructor: Constructor<GoogleChrome> = GoogleChrome::class.java.getConstructor()
        zeroArgConstructor.newInstance()
    }
}
