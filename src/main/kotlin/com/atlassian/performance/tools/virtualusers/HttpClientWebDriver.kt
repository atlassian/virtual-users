package com.atlassian.performance.tools.virtualusers

import com.google.common.util.concurrent.SettableFuture
import net.jcip.annotations.GuardedBy
import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.openqa.selenium.*
import org.openqa.selenium.interactions.Keyboard
import org.openqa.selenium.interactions.Mouse
import org.openqa.selenium.remote.*
import org.openqa.selenium.remote.internal.JsonToWebElementConverter
import java.util.concurrent.Future
import java.util.logging.Level

internal class HttpClientWebDriver : RemoteWebDriver() {
    private val httpClient: SettableFuture<CloseableHttpClient> = SettableFuture.create()

    internal fun getHttpClientFuture(): Future<CloseableHttpClient> {
        return httpClient
    }

    fun initHttpClient(userName: String, password: String) {
        if (httpClient.isDone.not()) {
            val provider: CredentialsProvider = BasicCredentialsProvider()
            val credentials: Credentials = UsernamePasswordCredentials(userName, password)
            provider.setCredentials(AuthScope.ANY, credentials)
            httpClient.set(
                HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build()
            )
        }
    }

    override fun executeScript(script: String?, vararg args: Any?): Any {
        throw Exception("not implemented")
    }

    override fun findElementById(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun setFileDetector(detector: FileDetector?) {
        throw Exception("not implemented")
    }

    override fun getSessionId(): SessionId {
        throw Exception("not implemented")
    }

    override fun log(sessionId: SessionId?, commandName: String?, toLog: Any?, `when`: When?) {
        throw Exception("not implemented")
    }

    override fun findElementByTagName(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun findElementByXPath(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun findElementsByXPath(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun getTitle(): String {
        throw Exception("not implemented")
    }

    override fun executeAsyncScript(script: String?, vararg args: Any?): Any {
        throw Exception("not implemented")
    }

    override fun getMouse(): Mouse {
        throw Exception("not implemented")
    }

    override fun close() {
        throw Exception("not implemented")
    }

    override fun findElementByPartialLinkText(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun getWindowHandles(): MutableSet<String> {
        throw Exception("not implemented")
    }

    override fun setElementConverter(converter: JsonToWebElementConverter?) {
        throw Exception("not implemented")
    }

    override fun getExecuteMethod(): ExecuteMethod {
        throw Exception("not implemented")
    }

    override fun setSessionId(opaqueKey: String?) {
        throw Exception("not implemented")
    }

    override fun findElementsById(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun <X : Any?> getScreenshotAs(outputType: OutputType<X>?): X {
        return outputType!!.convertFromBase64Png("")
    }

    override fun findElementsByPartialLinkText(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun toString(): String {
        throw Exception("not implemented")
    }

    override fun getErrorHandler(): ErrorHandler {
        throw Exception("not implemented")
    }

    override fun setFoundBy(context: SearchContext?, element: WebElement?, by: String?, using: String?) {
        throw Exception("not implemented")
    }

    override fun getCommandExecutor(): CommandExecutor {
        throw Exception("not implemented")
    }

    override fun findElementsByName(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun findElementsByCssSelector(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun findElements(by: By?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun findElements(by: String?, using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun getElementConverter(): JsonToWebElementConverter {
        throw Exception("not implemented")
    }

    override fun findElementByClassName(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun setLogLevel(level: Level?) {
        throw Exception("not implemented")
    }

    override fun findElementsByLinkText(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun findElementsByClassName(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun getPageSource(): String {
        return "none"
    }

    override fun setCommandExecutor(executor: CommandExecutor?) {
        throw Exception("not implemented")
    }

    override fun findElementByName(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun resetInputState() {
        throw Exception("not implemented")
    }

    override fun get(url: String?) {
        throw Exception("not implemented")
    }

    override fun findElement(by: By?): WebElement {
        return HttpClientWebElement()
    }

    override fun findElement(by: String?, using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun getWindowHandle(): String {
        throw Exception("not implemented")
    }

    override fun getFileDetector(): FileDetector {
        throw Exception("not implemented")
    }

    override fun execute(driverCommand: String?, parameters: MutableMap<String, *>?): Response {
        throw Exception("not implemented")
    }

    override fun execute(command: String?): Response {
        throw Exception("not implemented")
    }

    override fun navigate(): WebDriver.Navigation {
        throw Exception("not implemented")
    }

    override fun manage(): WebDriver.Options {
        throw Exception("not implemented")
    }

    override fun findElementByLinkText(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun getKeyboard(): Keyboard {
        throw Exception("not implemented")
    }

    override fun getCurrentUrl(): String {
        return "none"
    }

    override fun getCapabilities(): Capabilities {
        throw Exception("not implemented")
    }

    override fun findElementByCssSelector(using: String?): WebElement {
        throw Exception("not implemented")
    }

    override fun setErrorHandler(handler: ErrorHandler?) {
        throw Exception("not implemented")
    }

    override fun switchTo(): WebDriver.TargetLocator {
        throw Exception("not implemented")
    }

    override fun quit() {
        httpClient.get().close()
    }

    override fun findElementsByTagName(using: String?): MutableList<WebElement> {
        throw Exception("not implemented")
    }

    override fun startSession(capabilities: Capabilities?) {
        throw Exception("not implemented")
    }

    private class HttpClientWebElement : WebElement {
        override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X {
            throw Exception("not implemented")
        }

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
            return "text"
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

}
