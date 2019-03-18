package com.atlassian.performance.tools.virtualusers.lib.sshubuntu

import com.atlassian.performance.tools.virtualusers.lib.docker.execAsResource
import com.github.dockerjava.core.DockerClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class SudoSshUbuntuImageIT {

    @Test
    fun shouldStartUbuntu() {
        val docker = DockerClientBuilder.getInstance().build()
        val osName = docker
            .createNetworkCmd()
            .withName(UUID.randomUUID().toString())
            .execAsResource(docker).use { network ->
                SudoSshUbuntuImage(
                    docker,
                    network.response.id,
                    emptyList()
                ).runInUbuntu { ubuntu ->
                    ubuntu.ssh.newConnection().use { ssh ->
                        ssh.execute("sudo apt-get install -y lsb-release")
                        ssh.execute("sudo lsb_release -cs").output
                    }
                }
            }

        assertThat(osName).isEqualTo("xenial\n")
    }
}
