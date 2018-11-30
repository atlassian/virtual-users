package com.atlassian.performance.tools.virtualusers.mock

import org.openqa.selenium.*

class WebElementMock(
    private val text: String
) : WebElement {
    override fun isDisplayed(): Boolean {
        throw Exception("not implemented")
    }

    override fun clear() {
        throw Exception("not implemented")
    }

    override fun submit() {
        throw Exception("not implemented")
    }

    override fun getLocation(): Point {
        throw Exception("not implemented")
    }

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X {
        throw Exception("not implemented")
    }

    override fun findElement(by: By?): WebElement {
        throw Exception("not implemented")
    }

    override fun click() {
        throw Exception("not implemented")
    }

    override fun getTagName(): String {
        throw Exception("not implemented")
    }

    override fun getSize(): Dimension {
        throw Exception("not implemented")
    }

    override fun getText(): String {
        return text
    }

    override fun isSelected(): Boolean {
        throw Exception("not implemented")
    }

    override fun isEnabled(): Boolean {
        throw Exception("not implemented")
    }

    override fun sendKeys(vararg keysToSend: CharSequence?) {
        throw Exception("not implemented")
    }

    override fun getAttribute(name: String?): String {
        throw Exception("not implemented")
    }

    override fun getRect(): Rectangle {
        throw Exception("not implemented")
    }

    override fun getCssValue(propertyName: String?): String {
        throw Exception("not implemented")
    }

    override fun findElements(by: By?): MutableList<WebElement> {
        throw Exception("not implemented")
    }
}