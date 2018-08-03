package com.atlassian.performance.tools.virtualusers.browsers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.Dimension
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File

class GoogleChrome(
    private val driverRuntime: ChromedriverRuntime
) {

    private val logger: Logger = LogManager.getLogger(this::class.java)

    fun start(
        headless: Boolean = true,
        verboseLog: File? = null
    ): RemoteWebDriver {
        logger.debug("Starting Chrome")
        driverRuntime.ensureRunning()
        System.setProperty("webdriver.http.factory", "apache")
        val service = ChromeDriverService.Builder()
        if (verboseLog != null) {
            service
                .withLogFile(verboseLog)
                .withVerbose(true)
        }
        val driver = ChromeDriver(service.build(), getOptions(headless))
        driver.manage().window().size = Dimension(1024, 768)
        return driver
    }

    private fun getOptions(
        headless: Boolean
    ): ChromeOptions {
        val options = ChromeOptions()
        if (headless) {
            options.setHeadless()
        }
        options
            .disableSandbox()
            .disableInfobars()
            .setExperimentalOption(
            "prefs",
            mapOf(
                "credentials_enable_service" to false
            )
        )
        return options
    }

    /**
     * Additional --disable-gpu flag is necessary only on Windows.
     * https://bugs.chromium.org/p/chromium/issues/detail?id=737678
     */
    private fun ChromeOptions.setHeadless(): ChromeOptions {
        val osName = System.getProperty("os.name").toLowerCase()
        this.addArguments("--headless")
        if (osName.contains("win")) {
            this.addArguments("--disable-gpu")
        }
        return this
    }

    private fun ChromeOptions.disableSandbox(): ChromeOptions {
        this.addArguments("--no-sandbox")
        return this
    }

    private fun ChromeOptions.disableInfobars(): ChromeOptions {
        this.addArguments("--disable-infobars")
        return this
    }
}