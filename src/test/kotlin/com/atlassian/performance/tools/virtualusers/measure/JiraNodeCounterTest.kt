package com.atlassian.performance.tools.virtualusers.measure

import org.assertj.core.api.Assertions
import org.junit.Test
import java.lang.StringBuilder

class JiraNodeCounterTest {

    @Test
    fun shouldCountAFailingNode() {
        val jiraNodeCounter = JiraNodeCounter()

        jiraNodeCounter.count(
            BrokenApplicationNode()
        )

        val nodeIdentifyBuilder = StringBuilder()
        jiraNodeCounter.dump(nodeIdentifyBuilder)
        Assertions.assertThat(nodeIdentifyBuilder.toString().trim()).isEqualTo("unknown,1")
    }

    private class BrokenApplicationNode : ApplicationNode {
        override fun identify(): String {
            throw Exception("Failed to identify jira Node")
        }
    }
}
