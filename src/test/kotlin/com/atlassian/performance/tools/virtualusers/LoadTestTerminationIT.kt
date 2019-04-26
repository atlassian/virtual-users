package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import org.apache.catalina.core.StandardServer
import org.apache.catalina.startup.Tomcat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File
import java.net.URI
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.system.measureTimeMillis

class LoadTestTerminationIT {

    private val load = VirtualUserLoad.Builder()
        .virtualUsers(3)
        .hold(Duration.ZERO)
        .ramp(Duration.ZERO)
        .flat(Duration.ofSeconds(21))
        .build()

    @Test
    fun shouldHaveReasonableOverheadDespiteSlowNavigations() {
        val loadTest = prepareLoadTest(SlowShutdownBrowser::class.java)

        val termination = testTermination(loadTest, "shouldHaveReasonableOverheadDespiteSlowNavigations")

        assertThat(termination.overhead).isLessThan(Duration.ofSeconds(5) + LoadSegment.DRIVER_CLOSE_TIMEOUT)
        assertThat(termination.blockingThreads).isEmpty()
    }

    @Test
    fun shouldCloseAFastBrowser() {
        val loadTest = prepareLoadTest(FastShutdownBrowser::class.java)

        val termination = testTermination(loadTest, "shouldCloseAFastBrowser")

        assertThat(CLOSED_BROWSERS).contains(FastShutdownBrowser::class.java)
        assertThat(termination.overhead).isLessThan(Duration.ofSeconds(5))
        assertThat(termination.blockingThreads).isEmpty()
    }

    private fun prepareLoadTest(
        browser: Class<out Browser>
    ): LoadTest {
        val options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = URI("http://doesnt-matter"),
                userName = "u",
                password = "p"
            ),
            behavior = VirtualUserBehavior.Builder(NavigatingScenario::class.java)
                .skipSetup(true)
                .browser(browser)
                .load(load)
                .build()
        )
        return LoadTest(
            options = options,
            userGenerator = SuppliedUserGenerator()
        )
    }

    private fun testTermination(
        test: LoadTest,
        label: String
    ): TerminationResult {
        val threadGroup = ThreadGroup(label)
        val threadName = "parent-for-$label"
        val testDuration = measureTimeMillis {
            Executors.newSingleThreadExecutor {
                Thread(threadGroup, it, threadName)
            }.submit {
                test.run()
            }.get()
        }
        return TerminationResult(
            overhead = Duration.ofMillis(testDuration) - load.total,
            blockingThreads = threadGroup.listBlockingThreads().filter { it.name != threadName }
        )
    }

    private class TerminationResult(
        val overhead: Duration,
        /**
         * If you want to find out who created these threads, you can debug with a breakpoint on [Thread.start]
         * and filter e.g. by [Thread.getName].
         */
        val blockingThreads: List<Thread>
    )

    private fun ThreadGroup.listBlockingThreads(): List<Thread> {
        return listThreads().filter { it.isDaemon.not() }
    }

    private fun ThreadGroup.listThreads(): List<Thread> {
        val threads = Array<Thread?>(activeCount()) { null }
        enumerate(threads)
        return threads.toList().filterNotNull().filter { it.isAlive }
    }
}

private val CLOSED_BROWSERS: MutableList<Class<*>> = mutableListOf()

private class SlowShutdownBrowser : SlowNavigationBrowser() {
    override val shutdown: Duration = Duration.ofSeconds(120)
}

private class FastShutdownBrowser : SlowNavigationBrowser() {
    override val shutdown: Duration = Duration.ofMillis(500)
}

private abstract class SlowNavigationBrowser : Browser {
    private val navigation: Duration = Duration.ofSeconds(10)
    abstract val shutdown: Duration

    override fun start(): CloseableRemoteWebDriver {
        val parent = Executors.newSingleThreadExecutor {
            Thread(it)
                .apply { name = "parent-for-tomcat" }
                .apply { isDaemon = true }
        }
        val tomcat = parent.submit(Callable { startTomcat() }).get()
        val base = URI("http://localhost:${tomcat.connector.localPort}")
        val driver = RemoteWebDriver(base.toURL(), DesiredCapabilities())
        val clazz = this::class.java
        return object : CloseableRemoteWebDriver(driver) {
            override fun close() {
                super.close()
                Thread.sleep(shutdown.toMillis())
                tomcat.stop()
                CLOSED_BROWSERS.add(clazz)
            }
        }
    }

    private fun startTomcat(): Tomcat {
        val port = 8500 + PORT_OFFSET.getAndIncrement()
        val tomcat = Tomcat().apply { setPort(port) }
        val context = tomcat.addContext("", File(".").absolutePath)
        Tomcat.addServlet(context, "WD", MockWebDriverServer(navigation))
        context.addServletMappingDecoded("/*", "WD")
        (tomcat.server as StandardServer).utilityThreadsAsDaemon = true
        tomcat.start()
        tomcat.connector
        return tomcat
    }

    private companion object {
        val PORT_OFFSET = AtomicInteger(0)
    }
}

private class MockWebDriverServer(
    private val navigation: Duration
) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        when (req.pathInfo) {
            "/session" -> {
                resp.writer.use {
                    it.write(
                        """
                        {
                            "value": {
                                "sessionId": "123",
                                "capabilities": {}
                            }
                        }
                        """.trimIndent()
                    )
                }
            }
            "/session/123" -> {
            }
            "/session/123/url" -> {
                Thread.sleep(navigation.toMillis())
            }
            "/session/123/element" -> {
                resp.writer.use {
                    it.write("{}")
                }
            }
            else -> resp.sendError(500, "Not implemented")
        }
    }
}

private class NavigatingScenario : Scenario {

    override fun getLogInAction(
        jira: WebJira,
        meter: ActionMeter,
        userMemory: UserMemory
    ): Action = object : Action {
        override fun run() {}
    }

    override fun getActions(
        jira: WebJira,
        seededRandom: SeededRandom,
        meter: ActionMeter
    ): List<Action> = listOf(
        object : Action {
            private val navigation = ActionType("Navigation") { Unit }
            override fun run() {
                meter.measure(navigation) {
                    jira.navigateTo("whatever")
                }
            }
        }
    )
}
