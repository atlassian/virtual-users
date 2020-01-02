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
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.ssh.api.auth.PublicKeyAuthentication
import com.atlassian.performance.tools.sshubuntu.api.SshHost
import com.atlassian.performance.tools.sshubuntu.api.SshUbuntu
import com.atlassian.performance.tools.sshubuntu.api.SshUbuntuContainer
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf423WorkaroundOracleJdk
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf425WorkaroundMysqlDatabase
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.SshJiraNode
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import java.net.URI
import java.time.Duration
import java.util.concurrent.Executors
import java.util.function.Consumer

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
        )
    )


    private fun <T> testWithJira(
        test: (URI) -> T
    ): T {
        fun exposePort(
            port: Int
        ) = Consumer { container: GenericContainer<*> ->
            container.addExposedPort(port)
        }

        val dbContainer = SshUbuntuContainer(exposePort(3306))
        val jiraContainer = SshUbuntuContainer(exposePort(8080))
        return dbContainer.start().use { db ->
            jiraContainer.start().use { jira ->
                test(runJiraServer(jira, db))
            }
        }
    }

    private fun runJiraServer(
        jira: SshUbuntu,
        db: SshUbuntu
    ): URI {
        val publishedJiraPort = jira.container.getMappedPort(8080)
        val jiraUri = URI("http://localhost:$publishedJiraPort/")
        db.ssh.becomeUsable().newConnection().use {
            dataset.database.setup(it)
            dataset.database.start(jiraUri, it)
        }
        startJiraNode(jira.ssh.becomeUsable(), db.container.getContainerIpAddress())
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

    private fun SshHost.becomeUsable(): Ssh {
        return Ssh(com.atlassian.performance.tools.ssh.api.SshHost(
            ipAddress = ipAddress,
            userName = userName,
            authentication = PublicKeyAuthentication(privateKey),
            port = port
        )).also { ssh ->
            ssh.newConnection().use { shell ->
                shell.execute("apt-get update", Duration.ofMinutes(2))
                shell.execute("apt-get install sudo")
            }
        }
    }
}
