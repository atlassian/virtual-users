package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.dockerinfrastructure.api.jira.Jira
import com.atlassian.performance.tools.dockerinfrastructure.api.jira.JiraCoreFormula
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RestUserGeneratorIT {

    @Test
    fun shouldCreateFiveUsers() {
        val newUsers = JiraCoreFormula.Builder()
            .inDockerNetwork(false)
            .build()
            .provision()
            .use { RestUserGenerator(target(it)).generateUsers(5) }

        assertThat(newUsers).hasSize(5)
    }

    private fun target(
        jira: Jira
    ): VirtualUserTarget = VirtualUserTarget(
        webApplication = jira.getUri(),
        userName = "admin",
        password = "admin"
    )
}
