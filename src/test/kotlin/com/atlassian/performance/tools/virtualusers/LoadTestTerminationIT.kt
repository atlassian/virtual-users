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
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
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

        assertThat(termination.overhead).isLessThan(Duration.ofSeconds(2) + LoadSegment.DRIVER_CLOSE_TIMEOUT)
        assertThat(termination.blockingThreads).isEmpty()
    }

    @Test
    fun shouldCloseAFastBrowser() {
        val loadTest = prepareLoadTest(FastShutdownBrowser::class.java)

        val termination = testTermination(loadTest, "shouldCloseAFastBrowser")

        assertThat(termination.overhead).isLessThan(Duration.ofSeconds(2))
        assertThat(CLOSED_BROWSERS).contains(FastShutdownBrowser::class.java)
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
                .results(TestVuNode.isolateTestNode(javaClass))
                .build()
        )
        return LoadTest(options)
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
        Thread.sleep(200) // give the threads from the [test] a chance to die
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
        val browserPort = 8500 + PORT_OFFSET.getAndIncrement()
        val browser = MockHttpServer(browserPort, shutdown)
        browser.register("/session", HttpHandler { http ->
            val sessionResponse = """
                {
                    "value": {
                        "sessionId": "123",
                        "capabilities": {}
                    }
                }
                """.trimIndent()
            http.sendResponseHeaders(200, sessionResponse.length.toLong())
            http.responseBody.bufferedWriter().use { it.write(sessionResponse) }
            http.close()
        })
        browser.register("/session/123/url", HttpHandler { http ->
            Thread.sleep(navigation.toMillis())
            http.sendResponseHeaders(200, 0)
            http.close()
        })
        val startedBrowser = browser.start()
        val driver = RemoteWebDriver(browser.base.toURL(), DesiredCapabilities())
        val clazz = this::class.java
        return object : CloseableRemoteWebDriver(driver) {
            override fun close() {
                super.close()
                startedBrowser.close()
                CLOSED_BROWSERS.add(clazz)
            }
        }
    }

    private companion object {
        val PORT_OFFSET = AtomicInteger(0)
    }
}

private class MockHttpServer(
    private val port: Int,
    private val shutdownSlowness: Duration
) {
    private val handlers: MutableMap<String, HttpHandler> = mutableMapOf()
    internal val base = URI("http://localhost:$port")

    internal fun register(
        context: String,
        handler: HttpHandler
    ): URI {
        handlers[context] = handler
        return base.resolve(context)
    }

    internal fun start(): AutoCloseable {
        val executorService: ExecutorService = Executors.newCachedThreadPool {
            Thread(it)
                .apply { name = "mock-http" }
                .apply { isDaemon = true }
        }
        val server = startHttpServer(executorService)
        return AutoCloseable {
            executorService.shutdownNow()
            Thread.sleep(shutdownSlowness.toMillis())
            server.stop(0)
        }
    }

    private fun startHttpServer(executor: ExecutorService): HttpServer {
        val httpServer = HttpServer.create(InetSocketAddress(port), 0)
        httpServer.executor = executor

        handlers.forEach { (context, handler) ->
            httpServer.createContext(context).handler = handler
        }

        executor
            .submit { httpServer.start() }
            .get()
        return httpServer
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
            private val navigation = ActionType("Navigation") { }
            override fun run() {
                meter.measure(navigation) {
                    jira.navigateTo("whatever")
                }
            }
        }
    )
}
