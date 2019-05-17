package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import java.io.BufferedWriter
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.asStream

class VirtualUserResult internal constructor(
    vuPath: Path
) {
    private val parser = ActionMetricsParser()
    private val scenarioMetrics = vuPath.resolve("action-metrics.jpt")

    fun streamScenarioMetrics(): Stream<ActionMetric> {
        return scenarioMetrics
            .toFile()
            .inputStream()
            .use { parser.parse(it) }
            .asSequence()
            .asStream()
    }

    internal fun writeScenarioMetrics(): BufferedWriter {
        return scenarioMetrics
            .toFile()
            .ensureParentDirectory()
            .bufferedWriter()
    }
}
