package com.atlassian.performance.tools.virtualusers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.images.builder.ImageFromDockerfile
import java.net.URI

class JiraContainer(
    private val network: Network
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val jiraPort = 8080
    private val networkAlias = "jira"

    fun run(action: (jiraAddress: URI) -> Unit) {
        GenericContainerImpl(
            ImageFromDockerfile()
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("ubuntu:18.10")
                        .expose(jiraPort)
                        .run("apt-get", "update", "-qq")
                        .run("apt-get", "install", "wget", "openjdk-8-jdk", "-qq")
                        .run("wget", "https://product-downloads.atlassian.com/software/jira/downloads/atlassian-jira-core-7.12.3.tar.gz")
                        .run("tar", "-xzf", "atlassian-jira-core-7.12.3.tar.gz")
                        .run("rm", "/atlassian-jira-core-7.12.3-standalone/bin/check-java.sh")
                        .run("mkdir", "jira-home")
                        .env("JAVA_HOME", "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre")
                        .env("JIRA_HOME", "/jira-home")
                        .cmd("/atlassian-jira-core-7.12.3-standalone/bin/start-jira.sh", "-fg")
                        .build()
                }
        )
            .withExposedPorts(jiraPort)
            .withNetwork(network)
            .withNetworkAliases(networkAlias)
            .withLogConsumer { outputFrame ->
                val logLine = outputFrame.utf8String.replace(Regex("((\\r?\\n)|(\\r))$"), "")
                logger.info(logLine)
            }
            .waitingFor(
                LogWaitStrategy("The database is not yet configured")
            )
            .use { jiraContainer ->
                jiraContainer.start()
                val jiraUri = URI("http://$networkAlias:$jiraPort/")
                action(jiraUri)
            }
    }
}

/**
 * TestContainers depends on construction of recursive generic types like class C<SELF extends C<SELF>>. It doesn't work
 * in kotlin. See:
 * https://youtrack.jetbrains.com/issue/KT-17186
 * https://github.com/testcontainers/testcontainers-java/issues/318
 * The class is a workaround for the problem.
 */
private class GenericContainerImpl(dockerImageName: ImageFromDockerfile) : GenericContainer<GenericContainerImpl>(dockerImageName)