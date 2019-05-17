package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.io.api.directories
import java.nio.file.Path
import java.util.*

class VirtualUserNodeResult(
    nodePath: Path
) {
    private val vuResults = nodePath.resolve("test-results")
    internal val nodeDistribution = nodePath.resolve("nodes.csv")

    fun listResults(): List<VirtualUserResult> {
        return vuResults
            .toFile()
            .directories()
            .map { it.toPath() }
            .map { VirtualUserResult(it) }
    }

    internal fun isolateVuResult(
        vu: UUID
    ): VirtualUserResult {
        return vuResults
            .resolve(vu.toString())
            .let { VirtualUserResult(it) }
    }
}
