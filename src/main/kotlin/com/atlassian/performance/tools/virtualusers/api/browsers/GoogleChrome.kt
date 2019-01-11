package com.atlassian.performance.tools.virtualusers.api.browsers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.Dimension
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File

open class GoogleChrome : Browser {

    private val logger: Logger = LogManager.getLogger(GoogleChrome::class.java)
    private val driverRuntime: ChromedriverRuntime
    private val allowInsecureConnections: Boolean

    constructor() {
        this.driverRuntime = ChromedriverRuntime()
        allowInsecureConnections = false
    }

    override fun start(): CloseableRemoteWebDriver {
        val options = ChromeOptions()
        if (allowInsecureConnections) {
            options.addArguments("--ignore-certificate-errors")
        }
        logger.debug("Starting Chrome")
        driverRuntime.ensureRunning()
        System.setProperty("webdriver.http.factory", "apache")
        val service = ChromeDriverService.Builder()
        configure(options, service)
        val driver = ChromeDriver(service.build(), options)
        driver.manage().window().size = Dimension(1024, 768)
        return CloseableRemoteWebDriver(driver)
    }

    protected open fun configure(options: ChromeOptions, service: ChromeDriverService.Builder) {
        options
            .apply { addArguments("--no-sandbox") }
            .apply { addArguments("--disable-dev-shm-usage") }
            .apply { addArguments("--disable-infobars") }
            .setExperimentalOption(
                "prefs",
                mapOf(
                    "credentials_enable_service" to false
                )
            )
    }

    @Deprecated(
        message = "Do not use."
    )
    fun start(
        headless: Boolean = true,
        verboseLog: File? = null
    ): RemoteWebDriver {
        return object : GoogleChrome() {
            override fun configure(options: ChromeOptions, service: ChromeDriverService.Builder) {
                if (verboseLog != null) {
                    service
                        .withLogFile(verboseLog)
                        .withVerbose(true)
                }
                if (headless) {
                    HeadlessChromeBrowser.setHeadless(options)
                }
                super.configure(options, service)
            }
        }.start().getDriver()
    }

    @Deprecated(
        message = "Do not use."
    )
    constructor(driverRuntime: ChromedriverRuntime) {
        this.driverRuntime = driverRuntime
        this.allowInsecureConnections = false
    }

    @Deprecated(
        message = "Do not use."
    )
    constructor(
        driverRuntime: ChromedriverRuntime,
        allowInsecureConnections: Boolean
    ) {
        this.driverRuntime = driverRuntime
        this.allowInsecureConnections = allowInsecureConnections
    }
}
