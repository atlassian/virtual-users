package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.TestJira.MANY_USERS_JIRA
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.users.RestUserGenerator
import org.junit.Test
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Tests integration of [LoadTest] + [RestUserGenerator].
 */
class LoadTestUserCreationIT {

    @Test
    fun shouldCreateUsersInParallelDespiteBigUserBase() {
        val pool = Executors.newCachedThreadPool()
        val nodes = 6
        val load = VirtualUserLoad.Builder()
            .virtualUsers(75)
            .ramp(Duration.ofSeconds(45))
            .flat(Duration.ofSeconds(2))
            .maxOverallLoad(TemporalRate(1.0, Duration.ofSeconds(1)))
            .build()
        val loadSlices = load.slice(nodes)

        MANY_USERS_JIRA.runWithJira { jira ->
            (0 until nodes)
                .map { loadSlices[it] }
                .map { loadTest(jira, it, RestUserGenerator::class.java) }
                .map { pool.submit { it.run() } }
                .map { it.get() }
        }

        pool.shutdownNow()
    }

    private fun loadTest(
        jira: DockerJira,
        load: VirtualUserLoad,
        userGenerator: Class<RestUserGenerator>
    ): LoadTest = LoadTest(
        options = VirtualUserOptions(
            target = VirtualUserTarget(
                webApplication = jira.hostAddress,
                userName = "admin",
                password = "admin"
            ),
            behavior = VirtualUserBehavior.Builder(TracingScenario::class.java)
                .userGenerator(userGenerator)
                .browser(LoadTestTest.TestBrowser::class.java)
                .skipSetup(true)
                .results(TestVuNode.isolateTestNode(javaClass))
                .load(load)
                .build()
        )
    )
}
