package com.atlassian.performance.tools.virtualusers.api.browsers

import org.openqa.selenium.remote.RemoteWebDriver
import java.io.Closeable

open class CloseableRemoteWebDriver(
    private val driver: RemoteWebDriver
) : Closeable {

    fun getDriver(): RemoteWebDriver {
        return this.driver
    }

    override fun close() {
        driver.quit()
    }
}