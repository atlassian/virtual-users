package com.atlassian.performance.tools.virtualusers.lib.sshubuntu

import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.ssh.api.SshHost
import com.atlassian.performance.tools.ssh.api.auth.PasswordAuthentication
import com.atlassian.performance.tools.virtualusers.lib.docker.ConnectedContainer
import com.atlassian.performance.tools.virtualusers.lib.docker.CreatedContainer
import com.atlassian.performance.tools.virtualusers.lib.docker.StartedDockerContainer
import com.atlassian.performance.tools.virtualusers.lib.docker.execAsResource
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import java.time.Duration

/**
 * Supports:
 * * [Ssh] to an Ubuntu with `sudo`
 * * Docker in Docker
 * * granular Docker resource allocation and deallocation
 */
class SudoSshUbuntuImage(
    private val docker: DockerClient,
    private val networkId: String,
    private val portsToExpose: List<Int>
) {

    fun <T> runInUbuntu(
        lambda: (SudoSshUbuntuContainer) -> T
    ): T {
        docker
            .pullImageCmd("rastasheep/ubuntu-sshd")
            .withTag("16.04")
            .exec(PullImageResultCallback())
            .awaitCompletion()
        return docker
            .createContainerCmd("rastasheep/ubuntu-sshd:16.04")
            .withHostConfig(
                HostConfig()
                    .withPublishAllPorts(true)
                    .withPrivileged(true)
            )
            .withExposedPorts(
                portsToExpose.map { ExposedPort.tcp(it) }
            )
            .execAsResource(docker)
            .use { runInContainer(it, lambda) }
    }

    private fun <T> runInContainer(
        container: CreatedContainer,
        lambda: (SudoSshUbuntuContainer) -> T
    ): T = docker
        .connectToNetworkCmd()
        .withContainerId(container.response.id)
        .withNetworkId(networkId)
        .execAsResource(docker).use { runInConnectedContainer(it, lambda) }

    private fun <T> runInConnectedContainer(
        container: ConnectedContainer,
        lambda: (SudoSshUbuntuContainer) -> T
    ): T = docker
        .startContainerCmd(container.containerId)
        .execAsResource(docker)
        .use { runInStartedContainer(it, lambda) }

    private fun <T> runInStartedContainer(
        container: StartedDockerContainer,
        lambda: (SudoSshUbuntuContainer) -> T
    ): T {
        val networkSettings = docker
            .inspectContainerCmd(container.id)
            .exec()
            .networkSettings
        val ip = networkSettings.ipAddress
        val ports = networkSettings.ports
        val sshPort = ports
            .bindings[ExposedPort.tcp(22)]!!
            .single()
            .hostPortSpec
            .toInt()
        val sshHost = SshHost(
            ipAddress = "localhost",
            userName = "root",
            authentication = PasswordAuthentication("root"),
            port = sshPort
        )
        val ssh = Ssh(sshHost)
        ssh.newConnection().use {
            it.execute("apt-get update", Duration.ofMinutes(2))
            it.execute("apt-get install sudo")
        }
        return lambda(SudoSshUbuntuContainer(ssh, ports, ip))
    }
}
