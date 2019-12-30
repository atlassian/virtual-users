package com.atlassian.performance.tools.virtualusers.api.diagnostics

import com.atlassian.performance.tools.io.api.ensureDirectory
import org.apache.logging.log4j.LogManager
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File
import java.nio.file.Paths
import java.util.*

class WebDriverDiagnostics(
    private val driver: WebDriver,
    private val display: TakesScreenshot
) : Diagnostics {

    constructor(
        driver: RemoteWebDriver
    ) : this(
        driver = driver,
        display = driver
    )

    private val logger = LogManager.getLogger(this::class.java)

    override fun diagnose(
        exception: Exception
    ) {
        val dumpDir = Paths.get("diagnoses")
            .resolve(UUID.randomUUID().toString())
            .toFile()
            .ensureDirectory()
        logger.error("URL: ${driver.currentUrl}, ${dumpHtml(dumpDir)}, ${saveScreenshot(dumpDir)}, ${dumpLogs(dumpDir)}", exception)
    }

    private fun dumpLogs(dumpDirectory: File): String {
        val browserLogFile = File(dumpDirectory, "browser.log")
        val browserLog = driver.manage().logs().get(LogType.BROWSER)
            .joinToString("\n")
        browserLogFile.bufferedWriter().use { it.write(browserLog) }
        return "Browser log dumped at ${browserLogFile.path}"
    }

    private fun dumpHtml(
        dumpDirectory: File
    ): String {
        val htmlDump = File(dumpDirectory, "dump.html")
        htmlDump.bufferedWriter().use { it.write(getPageSource(driver)) }
        return "HTML dumped at ${htmlDump.path}"
    }

    //this retrieves the actual source instead of serialized DOM coming from driver.pageSource
    private fun getPageSource(driver: WebDriver): String {
        val javascriptExecutor = driver as JavascriptExecutor
        val executeScript = javascriptExecutor.executeScript("return document.documentElement.innerHTML")
        return executeScript as String
    }

    private fun saveScreenshot(
        dumpDirectory: File
    ): String {
        return try {
            val screenshot = File(dumpDirectory, "screenshot.png")
            val temporaryScreenshot = display.getScreenshotAs(OutputType.FILE)
            val moved = temporaryScreenshot.renameTo(screenshot)
            when {
                moved -> "screenshot saved to ${screenshot.path}"
                else -> "screenshot failed to migrate from ${temporaryScreenshot.path}"
            }
        } catch (e: Exception) {
            logger.error("Failed to take a screenshot", e)
            "screenshot failed"
        }
    }
}
