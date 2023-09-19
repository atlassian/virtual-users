package com.atlassian.performance.tools.virtualusers.api.browsers

@Deprecated("Include your browser/WebDriver logic in your LoadProcess")
interface Browser {
    fun start(): CloseableRemoteWebDriver
}
