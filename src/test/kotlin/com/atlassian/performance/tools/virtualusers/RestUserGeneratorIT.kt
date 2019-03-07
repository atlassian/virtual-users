package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.dockerinfrastructure.api.jira.Jira
import com.atlassian.performance.tools.dockerinfrastructure.api.jira.JiraCoreFormula
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class RestUserGeneratorIT {

    @Test
    fun shouldCreateUsersConcurrently() {
        val pool = Executors.newCachedThreadPool()
        val provisionedJira = JiraCoreFormula.Builder()
            .inDockerNetwork(false)
            .build()
            .provision()
        val jiraHttpPool = 150
        val vuNodes = jiraHttpPool / 2 + 1
        val usersPerNode = 12

        val newUsers = provisionedJira.use { jira ->
            val userGeneration = Callable {
                RestUserGenerator(target(jira)).generateUsers(usersPerNode)
            }
            (1..vuNodes)
                .map { pool.submit(userGeneration) }
                .map { it.get() }
                .flatten()
        }

        pool.shutdownNow()
        assertThat(newUsers).hasSize(vuNodes * usersPerNode)
    }

    private fun target(
        jira: Jira
    ): VirtualUserTarget = VirtualUserTarget(
        webApplication = jira.getUri(),
        userName = "admin",
        password = "admin"
    )
}
