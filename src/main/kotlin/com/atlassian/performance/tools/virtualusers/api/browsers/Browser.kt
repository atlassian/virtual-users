package com.atlassian.performance.tools.virtualusers.api.browsers

interface Browser {
    fun start(): CloseableRemoteWebDriver
}