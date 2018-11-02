package com.atlassian.performance.tools.virtualusers.api.browsers

import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions

/**
 * Runs Google chrome browser without GUI.
 */
open class HeadlessChromeBrowser : GoogleChrome() {
    override fun configure(options: ChromeOptions, service: ChromeDriverService.Builder) {
        setHeadless(options)
        super.configure(options, service)
    }

    internal companion object {
        fun setHeadless(options: ChromeOptions) {
            val osName = System.getProperty("os.name").toLowerCase()
            options.addArguments("--headless")
            if (osName.contains("win")) {
                options.addArguments("--disable-gpu")
            }
        }
    }

}