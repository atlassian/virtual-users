package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.mock.RemoteWebDriverMock
import com.atlassian.performance.tools.virtualusers.mock.WebElementMock
import org.junit.Test
import org.openqa.selenium.By
import java.net.URI
import java.time.Duration

class LoadTestTest {
    @Test
    fun shouldRunLoadTestWithoutExceptions() {
        val loadTest = LoadTest(
            options = VirtualUserOptions(
                target = VirtualUserTarget(
                    webApplication = URI("http://localhost:8080/"),
                    userName = "username",
                    password = "password"
                ),
                behavior = VirtualUserBehavior(
                    scenario = SleepingScenario::class.java,
                    load = VirtualUserLoad(
                        virtualUsers = 1,
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

        loadTest.run()
    }

    internal class TestWebDriver : RemoteWebDriverMock(mapOf(By.id("footer-build-information") to listOf(WebElementMock("jira-node"))))

    internal class TestBrowser : Browser {
        override fun start(): CloseableRemoteWebDriver {
            return CloseableRemoteWebDriver(
                driver = TestWebDriver()
            )
        }
    }
}