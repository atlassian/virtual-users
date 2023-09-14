package com.atlassian.performance.tools.virtualusers.measure

import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.function.Supplier

class ClusterNodeCounterTest {

    @Test
    fun shouldCountAFailingNode() {
        val clusterNodeCounter = ClusterNodeCounter()

        clusterNodeCounter.count(Supplier {
            throw Exception("Failed to identify jira Node")
        })

        val nodeIdentifyBuilder = StringBuilder()
        clusterNodeCounter.dump(nodeIdentifyBuilder)
        Assertions.assertThat(nodeIdentifyBuilder.toString().trim()).isEqualTo("unknown,1")
    }
}
