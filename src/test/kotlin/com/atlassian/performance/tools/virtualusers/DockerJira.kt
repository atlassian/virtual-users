package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.jira.JiraJvmArgs
import com.atlassian.performance.tools.infrastructure.api.jira.JiraLaunchTimeouts
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig
import com.atlassian.performance.tools.infrastructure.api.jvm.OracleJDK
import com.atlassian.performance.tools.infrastructure.api.profiler.AsyncProfiler
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.virtualusers.lib.docker.execAsResource
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf423WorkaroundOracleJdk
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.Jperf424WorkaroundJswDistro
import com.atlassian.performance.tools.virtualusers.lib.infrastructure.SshJiraNode
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuContainer
import com.atlassian.performance.tools.virtualusers.lib.sshubuntu.SudoSshUbuntuImage
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.core.DockerClientBuilder
import java.net.URI
import java.time.Duration
import java.util.*

class DockerJira(
    private val dataset: Dataset
) {

    fun <T> runWithJira(
        lambda: (URI) -> T
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
                        lambda(runJiraServer(jira, db))
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
            jiraDistro = Jperf424WorkaroundJswDistro("7.2.0"),
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
}
