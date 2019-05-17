package com.atlassian.performance.tools.virtualusers.lib.infrastructure

import com.atlassian.performance.tools.infrastructure.api.database.Database
import com.atlassian.performance.tools.infrastructure.api.database.MySqlDatabase
import com.atlassian.performance.tools.infrastructure.api.os.Ubuntu
import com.atlassian.performance.tools.ssh.api.SshConnection
import java.net.URI
import java.time.Duration

/**
 * Works around [JPERF-425](https://ecosystem.atlassian.net/browse/JPERF-425).
 */
class Jperf425WorkaroundMysqlDatabase(
    private val brokenMysqlDatabase: MySqlDatabase
) : Database {
    override fun setup(ssh: SshConnection): String {
        CopyPastedDocker().install(ssh)
        ssh.execute("service docker start")
        return brokenMysqlDatabase.setup(ssh)
    }

    override fun start(jira: URI, ssh: SshConnection) {
        return brokenMysqlDatabase.start(jira, ssh)
    }
}

/**
 * Copy-pasted verbatim from [infrastructure:4.11.0](https://github.com/atlassian/infrastructure/blob/release-4.11.0/src/main/kotlin/com/atlassian/performance/tools/infrastructure/Docker.kt)
 * It's here just to interject the Docker install and MySQL setup with a Docker start.
 * It also fixes `GPG error`.
 */
private class CopyPastedDocker {

    private val ubuntu = Ubuntu()

    /**
     * See the [official guide](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/#install-docker-ce).
     */
    fun install(
        ssh: SshConnection
    ) {
        ubuntu.install(
            ssh = ssh,
            packages = listOf(
                "apt-transport-https",
                "ca-certificates",
                "curl",
                "software-properties-common"
            ),
            timeout = Duration.ofMinutes(2)
        )
        val release = ssh.execute("lsb_release -cs").output
        val repository = "deb [arch=amd64] https://download.docker.com/linux/ubuntu $release stable"
        ssh.execute("curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -")
        ssh.execute("sudo add-apt-repository \"$repository\"")
        val version = "18.06.3~ce~3-0~ubuntu"
        ubuntu.install(
            ssh = ssh,
            packages = listOf("docker-ce=$version"),
            timeout = Duration.ofSeconds(180)
        )
    }
}
