package com.atlassian.performance.tools.virtualusers.api.diagnostics

import org.junit.Test
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.logging.LogEntries
import org.openqa.selenium.logging.Logs
import org.openqa.selenium.remote.RemoteWebDriver
import java.nio.file.Paths

class WebDriverDiagnosticsTest {

    @Test
    fun shouldLogRelativePath() {
        // given
        val driver = DriverMock()
        val display = DisplayMock()
        val diagnostics = WebDriverDiagnostics(driver, display)

        // when
        diagnostics.diagnose(Exception("Some exception"))

        // then
        // check log message manually
        Paths.get("diagnoses").toFile().deleteRecursively()
    }
}

internal class DriverMock : RemoteWebDriver() {
    override fun getCurrentUrl(): String {
        return "http://some.url/"
    }

    override fun executeScript(script: String, vararg args: Any?): Any {
        return "some html"
    }

    override fun manage(): WebDriver.Options {
        return object: RemoteWebDriverOptions() {
            override fun logs(): Logs {
                return LogsMock()
            }
        }
    }
}

internal class LogsMock : Logs {
    override fun getAvailableLogTypes(): MutableSet<String> {
        TODO("not implemented")
    }

    override fun get(logType: String?): LogEntries {
        return LogEntries(listOf())
    }
}

internal class DisplayMock: TakesScreenshot {
    override fun <X> getScreenshotAs(outputType: OutputType<X>): X {
        return outputType.convertFromBase64Png("some-screenshot")
    }
}
