package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.users.UserGenerator
import com.atlassian.performance.tools.virtualusers.mock.RemoteWebDriverMock
import com.atlassian.performance.tools.virtualusers.mock.WebElementMock
import net.jcip.annotations.ThreadSafe
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openqa.selenium.By
import java.net.URI
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@ThreadSafe
class LoadTestTest {

    /**
     * Prevents concurrent shared global state mutations if the test methods are concurrent.
     */
    private val globalStateLock = Object()

    @Test
    fun shouldRunLoadTestWithoutExceptions() {
        val loadTest = loadTest(
            virtualUsers = 6,
            skipSetup = false,
            createUsers = false
        )

        synchronized(globalStateLock) {
            TestBrowser.reset()
            loadTest.run()
        }

        assertThat(TestBrowser.timesStarted.get()).isEqualTo(7)
        assertThat(TracingScenario.setup).isEqualTo(true)
    }

    @Test
    fun shouldInstallOnlyOnce() {
        val loadTest = loadTest(
            virtualUsers = 20,
            skipSetup = false,
            createUsers = false
        )

        synchronized(globalStateLock) {
            MockWebdriverRuntime.reset()
            HardcodedUserGenerator.reset()
            loadTest.run()
        }

        assertThat(MockWebdriverRuntime.installations.get()).isEqualTo(1)
    }

    @Test
    fun shouldSkipSetup() {
        val loadTest = loadTest(
            virtualUsers = 4,
            skipSetup = true,
            createUsers = false
        )

        synchronized(globalStateLock) {
            TestBrowser.reset()
            TracingScenario.reset()
            HardcodedUserGenerator.reset()
            loadTest.run()
        }

        assertThat(TestBrowser.timesStarted.get()).isEqualTo(4)
        assertThat(TracingScenario.setup).isEqualTo(false)
    }

    @Test
    fun shouldCreateFiveUsers() {
        val loadTest = loadTest(
            virtualUsers = 5,
            skipSetup = true,
            createUsers = true
        )

        synchronized(globalStateLock) {
            TestBrowser.reset()
            TracingScenario.reset()
            HardcodedUserGenerator.reset()
            loadTest.run()
        }

        assertThat(HardcodedUserGenerator.usersCreated.get()).isEqualTo(5)
        assertThat(TracingScenario.users.count()).isEqualTo(5)
    }

    @Test
    fun shouldCreateTwelveUsers() {
        val loadTest = loadTest(
            virtualUsers = 12,
            skipSetup = true,
            createUsers = true
        )

        synchronized(globalStateLock) {
            TestBrowser.reset()
            TracingScenario.reset()
            HardcodedUserGenerator.reset()
            loadTest.run()
        }

        assertThat(HardcodedUserGenerator.usersCreated.get()).isEqualTo(12)
    }

    private fun loadTest(
        virtualUsers: Int,
        skipSetup: Boolean,
        createUsers: Boolean
    ): LoadTest = LoadTest(
        options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = URI("http://localhost:8080/"),
                userName = "username",
                password = "password"
            ),
            behavior = VirtualUserBehavior.Builder(TracingScenario::class.java)
                .browser(TestBrowser::class.java)
                .load(
                    VirtualUserLoad.Builder()
                        .virtualUsers(virtualUsers)
                        .hold(Duration.ZERO)
                        .ramp(Duration.ZERO)
                        .flat(Duration.ofSeconds(1))
                        .build()
                )
                .skipSetup(skipSetup)
                .createUsers(createUsers)
                .userGenerator(HardcodedUserGenerator::class.java)
                .results(TestVuNode.isolateTestNode(javaClass))
                .build()
        )
    )

    private class HardcodedUserGenerator : UserGenerator {

        companion object Counter {
            val usersCreated: AtomicInteger = AtomicInteger(0)

            fun reset() {
                usersCreated.set(0)
            }
        }

        override fun generateUser(options: VirtualUserOptions): User {
            return User("admin-${usersCreated.incrementAndGet()}", "admin")
        }
    }

    internal class TestWebDriver : RemoteWebDriverMock(mapOf(By.id("footer-build-information") to listOf(WebElementMock("jira-node"))))

    internal class TestBrowser : Browser {

        private val driverRuntime = MockWebdriverRuntime()

        override fun start(): CloseableRemoteWebDriver {
            driverRuntime.ensureRunning()
            timesStarted.incrementAndGet()
            return CloseableRemoteWebDriver(
                driver = TestWebDriver()
            )
        }

        companion object Counter {
            val timesStarted: AtomicInteger = AtomicInteger(0)

            fun reset() {
                timesStarted.set(0)
            }
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
