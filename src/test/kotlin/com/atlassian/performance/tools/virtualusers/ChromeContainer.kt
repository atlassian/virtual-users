package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import net.jcip.annotations.NotThreadSafe
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

@NotThreadSafe
internal class ChromeContainer : Browser {
    private val browser: BrowserWebDriverContainerImpl = BrowserWebDriverContainerImpl()
        .withDesiredCapabilities(DesiredCapabilities.chrome())
        .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)
        .waitingFor(HostPortWaitStrategy())
        .withNetwork(SharedNetwork("entry-point-test-network"))
        .withExposedPorts(4444)

    override fun start(): CloseableRemoteWebDriver {
        browser.start()
        return object : CloseableRemoteWebDriver(browser.webDriver) {
            override fun close() {
                super.close()
                browser.stop()
            }
        }
    }

}


/**
 * TestContainers depends on construction of recursive generic types like class C<SELF extends C<SELF>>. It doesn't work
 * in kotlin. See:
 * https://youtrack.jetbrains.com/issue/KT-17186
 * https://github.com/testcontainers/testcontainers-java/issues/318
 * The class is a workaround for the problem.
 */
internal class BrowserWebDriverContainerImpl() : BrowserWebDriverContainer<BrowserWebDriverContainerImpl>()
