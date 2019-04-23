package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraJvmArgs
import com.atlassian.performance.tools.infrastructure.api.jira.JiraLaunchTimeouts
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig
import com.atlassian.performance.tools.infrastructure.api.jvm.OracleJDK
import com.atlassian.performance.tools.infrastructure.api.profiler.AsyncProfiler
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.lib.docker.execAsResource
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf423WorkaroundOracleJdk
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.SshJiraNode
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuContainer
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuImage
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.core.DockerClientBuilder
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.junit.Test
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoadTestIT {

    private val dataset: Dataset = URI("https://s3-eu-central-1.amazonaws.com/")
        .resolve("jpt-custom-datasets-storage-a008820-datasetbucket-dah44h6l1l8p/")
        .resolve("jsw-7.13.0-100k-users-sync/")
        .let { bucket ->
            Dataset(
                database = Jperf425WorkaroundMysqlDatabase(MySqlDatabase(
                    HttpDatasetPackage(
                        uri = bucket.resolve("database.tar.bz2"),
                        downloadTimeout = Duration.ofMinutes(6)
                    )
                )),
                jiraHomeSource = JiraHomePackage(HttpDatasetPackage(
                    uri = bucket.resolve("jirahome.tar.bz2"),
                    downloadTimeout = Duration.ofMinutes(6)
                )),
                label = "100k users"
            )
        }

    private val behavior = VirtualUserBehavior.Builder(TracingScenario::class.java)
        .createUsers(true)
        .browser(LoadTestTest.TestBrowser::class.java)
        .skipSetup(true)

    @Test
    fun shouldCreateUsersInParallelDespiteBigUserBase() {
        val pool = Executors.newCachedThreadPool()
        val nodes = 6
        val load = VirtualUserLoad.Builder()
            .virtualUsers(75)
            .ramp(Duration.ofSeconds(45))
            .flat(Duration.ofMinutes(3))
            .maxOverallLoad(TemporalRate(15.0, Duration.ofSeconds(1)))
            .build()
        val loadSlices = load.slice(nodes)

        testWithJira { jira ->
            (0 until nodes)
                .map { loadSlices[it] }
                .map { loadTest(jira, it) }
                .map { pool.submit { it.run() } }
                .map { it.get() }
        }

        pool.shutdownNow()
    }

    private fun loadTest(
        jira: URI,
        load: VirtualUserLoad
    ): LoadTest = LoadTest(
        options = VirtualUserOptions(
            target = target(jira),
            behavior = behavior
                .load(load)
                .build()
        ),
        userGenerator = RestUserGenerator()
    )


    private fun <T> testWithJira(
        test: (URI) -> T
    ): T {
        val docker = DockerClientBuilder.getInstance().build()
        return docker
            .createNetworkCmd()
            .withName(UUID.randomUUID().toString())
            .execAsResource(docker)
            .use { network ->
                val networkId = network.response.id
                val dbImage = SudoSshUbuntuImage(docker, networkId, listOf(3306))
                val jiraImage = SudoSshUbuntuImage(docker, networkId, listOf(8080))
                dbImage.runInUbuntu { db ->
                    jiraImage.runInUbuntu { jira ->
                        test(runJiraServer(jira, db))
                    }
                }
            }
    }

    private fun runJiraServer(
        jira: SudoSshUbuntuContainer,
        db: SudoSshUbuntuContainer
    ): URI {
        val publishedJiraPort = jira.ports.bindings[ExposedPort.tcp(8080)]!!.single().hostPortSpec.toInt()
        val jiraUri = URI("http://localhost:$publishedJiraPort/")
        db.ssh.newConnection().use {
            dataset.database.setup(it)
            dataset.database.start(jiraUri, it)
        }
        startJiraNode(jira.ssh, db.peerIp)
        return jiraUri
    }

    private fun startJiraNode(
        jiraSsh: Ssh,
        dbIp: String
    ) {
        SshJiraNode(
            sshHost = jiraSsh,
            jiraDistro = Jperf424WorkaroundJswDistro("7.13.0"),
            config = JiraNodeConfig.Builder()
                .jvmArgs(JiraJvmArgs(xmx = "2g")) // make sure your Docker Engine has more memory than that
                .profiler(AsyncProfiler())
                .launchTimeouts(
                    JiraLaunchTimeouts.Builder()
                        .initTimeout(Duration.ofMinutes(7))
                        .build()
                )
                .build(),
            jiraHomeSource = dataset.jiraHomeSource,
            databaseIp = dbIp,
            launchTimeouts = JiraLaunchTimeouts.Builder().build(),
            jdk = Jperf423WorkaroundOracleJdk(OracleJDK())
        ).start()
    }

    private fun target(
        jira: URI
    ): VirtualUserTarget = VirtualUserTarget(
        webApplication = jira,
        userName = "admin",
        password = "admin"
    )

    @Test
    fun shouldTerminateDespiteSlowUninterruptibleNavigation() {
        val options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = URI("http://doesnt-matter"),
                userName = "u",
                password = "p"
            ),
            behavior = VirtualUserBehavior.Builder(NavigatingScenario::class.java)
                .skipSetup(true)
                .browser(SlowBrowser::class.java)
                .load(
                    VirtualUserLoad.Builder()
                        .virtualUsers(1)
                        .hold(Duration.ZERO)
                        .ramp(Duration.ZERO)
                        .flat(Duration.ofSeconds(21))
                        .build()
                )
                .build()
        )
        val loadTest = LoadTest(
            options = options,
            userGenerator = SuppliedUserGenerator()
        )

        loadTest.run()
    }
}

private class SlowBrowser : Browser {
    private val navigationSlowness = Duration.ofSeconds(10)

    override fun start(): CloseableRemoteWebDriver {
        val browserPort = 8500
        val browser = MockHttpServer(browserPort)
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
            Thread.sleep(navigationSlowness.toMillis())
            http.sendResponseHeaders(200, 0)
            http.close()
        })
        val startedBrowser = browser.start()
        val driver = RemoteWebDriver(browser.base.toURL(), DesiredCapabilities())
        return object : CloseableRemoteWebDriver(driver) {
            override fun close() {
                super.close()
                startedBrowser.close()
            }
        }
    }
}

private class MockHttpServer(private val port: Int) {
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
        val executorService: ExecutorService = Executors.newCachedThreadPool()
        val server = startHttpServer(executorService)
        return AutoCloseable {
            executorService.shutdownNow()
            server.stop(2)
        }
    }

    private fun startHttpServer(executor: Executor): HttpServer {
        val httpServer = HttpServer.create(InetSocketAddress(port), 0)
        httpServer.executor = executor

        handlers.forEach { context, handler ->
            httpServer.createContext(context).handler = handler
        }

        httpServer.start()
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
            private val navigation = ActionType("Navigation") { Unit }
            override fun run() {
                meter.measure(navigation) {
                    jira.navigateTo("whatever")
                }
            }
        }
    )
}
