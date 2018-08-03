package com.atlassian.performance.tools.virtualusers.browsers

import io.github.bonigarcia.wdm.ChromeDriverManager
import net.jcip.annotations.ThreadSafe

@ThreadSafe
class ChromedriverRuntime {

    private var running = false

    fun ensureRunning() {
        synchronized(this) {
            if (!running) {
                ChromeDriverManager.getInstance().version("2.39").setup()
                running = true
            }
        }
    }
}