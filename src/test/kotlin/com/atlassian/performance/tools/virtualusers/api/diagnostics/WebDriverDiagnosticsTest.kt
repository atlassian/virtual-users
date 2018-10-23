package com.atlassian.performance.tools.virtualusers.api.diagnostics

import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.lang.Exception
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

internal class DriverMock: WebDriver {
    override fun getCurrentUrl(): String {
        return "http://some.url/"
    }

    override fun getPageSource(): String {
        return "some html"
    }

    override fun quit() {
        throw Exception("unexpected call")
    }

    override fun close() {
        throw Exception("unexpected call")
    }

    override fun switchTo(): WebDriver.TargetLocator {
        throw Exception("unexpected call")
    }

    override fun get(p0: String?) {
        throw Exception("unexpected call")
    }

    override fun manage(): WebDriver.Options {
        throw Exception("unexpected call")
    }

    override fun navigate(): WebDriver.Navigation {
        throw Exception("unexpected call")
    }

    override fun getWindowHandle(): String {
        throw Exception("unexpected call")
    }

    override fun findElement(p0: By?): WebElement {
        throw Exception("unexpected call")
    }

    override fun getWindowHandles(): MutableSet<String> {
        throw Exception("unexpected call")
    }

    override fun findElements(p0: By?): MutableList<WebElement> {
        throw Exception("unexpected call")
    }

    override fun getTitle(): String {
        throw Exception("unexpected call")
    }
}

internal class DisplayMock: TakesScreenshot {
    override fun <X> getScreenshotAs(outputType: OutputType<X>): X {
        return outputType.convertFromBase64Png("some-screenshot")
    }
}