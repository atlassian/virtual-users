package com.atlassian.performance.tools.virtualusers.api.browsers

import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions

class VerboseChromeBrowser : HeadlessChromeBrowser() {
    override fun start(vuResult: VirtualUserResult): CloseableRemoteWebDriver {
        val logFile = vuResult.getVuPath()
            .ensureDirectory()
            .resolve("chrome-driver.log")
            .toFile()

        return object : HeadlessChromeBrowser() {
            override fun configure(options: ChromeOptions, service: ChromeDriverService.Builder) {
                service
                    .withLogFile(logFile)
                    .withVerbose(true)
                super.configure(options, service)
            }
        }.start()
    }
}
