package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.io.api.directories
import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.virtualusers.measure.JiraNodeCounter
import java.io.BufferedWriter
import java.nio.file.Path

/**
 * Points to results of multiple virtual users from a single VU node.
 *
 * @since 3.12.0
 */
class VirtualUserNodeResult(
    nodePath: Path
) {
    private val vuResults = nodePath.resolve("test-results")
    internal val nodeDistribution = vuResults.resolve("nodes.csv")

    fun listResults(): List<VirtualUserResult> {
        return vuResults
            .toFile()
            .directories()
            .map { it.toPath() }
            .map { VirtualUserResult(it) }
    }

    internal fun isolateVuResult(
        vu: String
    ): VirtualUserResult {
        return vuResults
            .resolve(vu)
            .let { VirtualUserResult(it) }
    }

    /**
     * @return how many VUs visited a given application node (e.g. Jira node id)
     */
    fun countVusPerNode() : Map<String, Int> {
        return nodeDistribution.toFile().bufferedReader().use { reader ->
            JiraNodeCounter().parse(reader)
        }
    }

    internal fun writeNodeCounts(): BufferedWriter = nodeDistribution
        .toFile()
        .ensureParentDirectory()
        .bufferedWriter()
}
