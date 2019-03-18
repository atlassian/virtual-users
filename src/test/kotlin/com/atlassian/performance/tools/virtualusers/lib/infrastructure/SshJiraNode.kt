package com.atlassian.performance.tools.virtualusers.lib.infrastructure

import com.atlassian.performance.tools.infrastructure.api.Sed
import com.atlassian.performance.tools.infrastructure.api.distribution.ProductDistribution
import com.atlassian.performance.tools.infrastructure.api.jira.*
import com.atlassian.performance.tools.infrastructure.api.jvm.JavaDevelopmentKit
import com.atlassian.performance.tools.jvmtasks.api.TaskTimer.time
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.ssh.api.SshConnection
import java.net.URI
import java.time.Duration
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import java.time.Instant.now

/**
 * Works around [JPERF-273](https://ecosystem.atlassian.net/browse/JPERF-273).
 */
class SshJiraNode(
    private val sshHost: Ssh,
    private val jiraDistro: ProductDistribution,
    private val config: JiraNodeConfig,
    private val jiraHomeSource: JiraHomeSource,
    private val databaseIp: String,
    private val launchTimeouts: JiraLaunchTimeouts,
    private val jdk: JavaDevelopmentKit
) {
    fun start() {
        sshHost.newConnection().use { start(it) }
    }

    private fun start(
        ssh: SshConnection
    ) {
        val installationPath = installJira(ssh)
        startJira(ssh, installationPath)
        waitForUpgrades(ssh)
    }

    private fun installJira(
        ssh: SshConnection
    ): String {
        jdk.install(ssh)
        val installedProduct = jiraDistro.install(ssh, ".")
        val jiraHome = time("download Jira home") { jiraHomeSource.download(ssh) }
        replaceDbconfigUrl(ssh, "$jiraHome/dbconfig.xml")
        SetenvSh(installedProduct).setup(
            connection = ssh,
            config = config,
            gcLog = JiraGcLog(installedProduct),
            jiraIp = sshHost.host.ipAddress
        )
        ssh.execute("echo jira.home=`realpath $jiraHome` > $installedProduct/atlassian-jira/WEB-INF/classes/jira-application.properties")
        ssh.execute("echo jira.autoexport=false > $jiraHome/jira-config.properties")
        ssh.execute("wget -q https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.40.tar.gz")
        ssh.execute("tar -xzf mysql-connector-java-5.1.40.tar.gz")
        ssh.execute("cp mysql-connector-java-5.1.40/mysql-connector-java-5.1.40-bin.jar $installedProduct/lib")
        return installedProduct
    }

    private fun replaceDbconfigUrl(
        connection: SshConnection,
        dbconfigXml: String
    ) {
        Sed().replace(
            connection = connection,
            expression = "(<url>.*(@(//)?|//))" + "([^:/]+)" + "(.*</url>)",
            output = """\1$databaseIp\5""",
            file = dbconfigXml
        )
    }

    private fun startJira(
        ssh: SshConnection,
        installationPath: String
    ) {
        ssh.execute(
            """
            |${jdk.use()}
            |./$installationPath/bin/start-jira.sh
            """.trimMargin(),
            ofMinutes(1)
        )
    }

    private fun waitForUpgrades(
        ssh: SshConnection
    ) {
        val upgradesEndpoint = URI("http://admin:admin@localhost:8080/rest/api/2/upgrade")
        waitForStatusToChange(
            statusQuo = "000",
            timeout = launchTimeouts.offlineTimeout,
            ssh = ssh,
            uri = upgradesEndpoint
        )
        waitForStatusToChange(
            statusQuo = "503",
            timeout = launchTimeouts.initTimeout,
            ssh = ssh,
            uri = upgradesEndpoint
        )
        ssh.execute(
            cmd = "curl --silent --retry 6 -X POST $upgradesEndpoint",
            timeout = ofSeconds(15)
        )
        waitForStatusToChange(
            statusQuo = "303",
            timeout = launchTimeouts.upgradeTimeout,
            ssh = ssh,
            uri = upgradesEndpoint
        )
    }

    private fun waitForStatusToChange(
        statusQuo: String,
        uri: URI,
        timeout: Duration,
        ssh: SshConnection
    ) {
        val backoff = ofSeconds(10)
        val deadline = now() + timeout
        while (true) {
            val currentStatus = ssh
                .safeExecute(
                    cmd = "curl --silent --write-out '%{http_code}' --output /dev/null -X GET $uri",
                    timeout = launchTimeouts.unresponsivenessTimeout
                )
                .output
            if (currentStatus != statusQuo) {
                break
            }
            if (deadline < now()) {
                throw Exception("$uri failed to get out of $statusQuo status within $timeout")
            }
            Thread.sleep(backoff.toMillis())
        }
    }
}
