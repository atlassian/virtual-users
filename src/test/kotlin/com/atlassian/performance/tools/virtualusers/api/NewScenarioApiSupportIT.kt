package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.virtualusers.NewLoadTest
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.SimpleWebdriverScenario
import org.junit.Test
import java.net.URI
import java.time.Duration

class NewScenarioApiSupportIT {

    @Test
    fun shouldConsumeHttpClientBasedScenario() {
        val loadTest = NewLoadTest(
            VirtualUserOptions(
                target = VirtualUserTarget(
                    webApplication = URI("http://localhost:8090/jira/"),
                    userName = "admin",
                    password = "admin"
                ),
                behavior = VirtualUserBehavior.Builder(
                    SimpleWebdriverScenario::class.java
                ).load(
                    VirtualUserLoad.Builder()
                        .virtualUsers(1)
                        .ramp(Duration.ZERO)
                        .flat(Duration.ofSeconds(120))
                        .build()
                ).build()
            )
        )

        loadTest.run()
    }

}
