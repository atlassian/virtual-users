package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.dataset.HttpDatasetPackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomePackage
import com.atlassian.performance.tools.infrastructure.api.jira.JiraJvmArgs
import com.atlassian.performance.tools.infrastructure.api.jira.JiraLaunchTimeouts
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig
import com.atlassian.performance.tools.infrastructure.api.jvm.OpenJDK
import com.atlassian.performance.tools.infrastructure.api.profiler.AsyncProfiler
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.lib.docker.execAsResource
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.SshJiraNode
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuContainer
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuImage
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkHttpDockerCmdExecFactory
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

class LoadTestUserCreationIT {

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
            .flat(Duration.ofSeconds(2))
            .maxOverallLoad(TemporalRate(1.0, Duration.ofSeconds(1)))
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
        )
    )


    private fun <T> testWithJira(
        test: (URI) -> T
    ): T {
        val docker = DockerClientImpl.getInstance().withDockerCmdExecFactory(OkHttpDockerCmdExecFactory())
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
            jdk = OpenJDK()
        ).start()
    }

    private fun target(
        jira: URI
    ): VirtualUserTarget = VirtualUserTarget(
        webApplication = jira,
        userName = "admin",
        password = "admin"
    )
}
