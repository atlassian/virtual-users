package com.atlassian.performance.tools.virtualusers.mock

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.RemoteWebDriver

internal open class RemoteWebDriverMock(
    private val elements : Map<By, List<WebElement>>
) : RemoteWebDriver() {
    override fun getWindowHandles(): MutableSet<String> {
        throw Exception("not implemented")
    }

    override fun findElement(by: By?): WebElement {
        return elements[by]!!.single()
    }

    override fun getWindowHandle(): String {
        throw Exception("not implemented")
    }

    override fun getPageSource(): String {
        throw Exception("not implemented")
    }

    override fun navigate(): WebDriver.Navigation {
        throw Exception("not implemented")
    }

    override fun manage(): WebDriver.Options {
        throw Exception("not implemented")
    }

    override fun getCurrentUrl(): String {
        throw Exception("not implemented")
    }

    override fun getTitle(): String {
        throw Exception("not implemented")
    }

    override fun get(url: String?) {
        throw Exception("not implemented")
    }

    override fun switchTo(): WebDriver.TargetLocator {
        throw Exception("not implemented")
    }

    override fun close() {
        throw Exception("not implemented")
    }

    override fun quit() {
        Thread.sleep(10)
    }

    override fun findElements(by: By?): MutableList<WebElement> {
        throw Exception("not implemented")
    }
}