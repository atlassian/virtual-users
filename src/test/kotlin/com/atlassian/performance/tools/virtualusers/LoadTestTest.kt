package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.mock.RemoteWebDriverMock
import com.atlassian.performance.tools.virtualusers.mock.WebElementMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openqa.selenium.By
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class LoadTestTest {

    private val globalStateLock = Object()

    @Test
    fun shouldRunLoadTestWithoutExceptions() {
        val loadTest = loadTest(1)

        synchronized(globalStateLock) {
            loadTest.run()
        }
    }

    @Test
    fun shouldInstallOnlyOnce() {
        val loadTest = loadTest(20)

        synchronized(globalStateLock) {
            MockWebdriverRuntime.reset()
            loadTest.run()
        }

        assertThat(MockWebdriverRuntime.installations.get()).isEqualTo(1)
    }

    private fun loadTest(
        virtualUsers: Int
    ): LoadTest = LoadTest(
        options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = URI("http://localhost:8080/"),
                userName = "username",
                password = "password"
            ),
            behavior = VirtualUserBehavior(
                scenario = NoopScenario::class.java,
                load = VirtualUserLoad(
                    virtualUsers = virtualUsers,
                    hold = Duration.ZERO,
                    ramp = Duration.ZERO,
                    flat = Duration.ofSeconds(1)
                ),
                diagnosticsLimit = 0,
                seed = 1,
                browser = TestBrowser::class.java
            )
        )
    )

    internal class TestWebDriver : RemoteWebDriverMock(mapOf(By.id("footer-build-information") to listOf(WebElementMock("jira-node"))))

    internal class TestBrowser : Browser {

        private val driverRuntime = MockWebdriverRuntime()

        override fun start(): CloseableRemoteWebDriver {
            driverRuntime.ensureRunning()
            return CloseableRemoteWebDriver(
                driver = TestWebDriver()
            )
        }
    }

    /**
     * Mocks [com.atlassian.performance.tools.virtualusers.api.browsers.ChromedriverRuntime].
     */
    internal class MockWebdriverRuntime {

        private var running = false

        fun ensureRunning() {
            synchronized(this) {
                if (!running) {
                    installations.incrementAndGet()
                    running = true
                }
            }
        }

        companion object Counter {
            val installations = AtomicInteger(0)

            fun reset() {
                installations.set(0)
            }
        }
    }
}