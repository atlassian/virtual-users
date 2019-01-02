package com.atlassian.performance.tools.virtualusers.api.browsers

import io.github.bonigarcia.wdm.ChromeDriverManager
import net.jcip.annotations.ThreadSafe
import org.openqa.selenium.os.ExecutableFinder

@ThreadSafe
class ChromedriverRuntime {

    private var running = false

    fun ensureRunning() {
        synchronized(this) {
            val chromedriverInstalled = ExecutableFinder().find("chromedriver") != null
            if (!chromedriverInstalled && !running) {
                ChromeDriverManager.getInstance().version("2.43").setup()
                running = true
            }
        }
    }
}