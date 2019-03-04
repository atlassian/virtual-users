package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.dockerinfrastructure.api.jira.Jira
import com.atlassian.performance.tools.dockerinfrastructure.api.jira.JiraCoreFormula
import com.atlassian.performance.tools.virtualusers.ChromeContainer
import com.atlassian.performance.tools.virtualusers.SimpleScenario
import org.junit.Test

class EntryPointIT {

    @Test
    fun shouldRunWith3_2_0_Args() {
        JiraCoreFormula.Builder()
            .build()
            .provision()
            .use { jira : Jira ->
                com.atlassian.performance.tools.virtualusers.api.main(arrayOf(
                    "--jira-address", jira.getUri().toString(),
                    "--login", "admin",
                    "--password", "admin",
                    "--virtual-users", "1",
                    "--hold", "PT0S",
                    "--ramp", "PT0S",
                    "--flat", "PT2M",
                    "--scenario", SimpleScenario::class.java.name,
                    "--browser", ChromeContainer::class.java.name,
                    "--diagnostics-limit", "64",
                    "--seed", "-9183767962456348780"
                ))
            }
    }
}