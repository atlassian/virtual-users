package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import java.io.BufferedWriter
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Points to results produced by a single virtual user.
 *
 * @since 3.12.0
 */
class VirtualUserResult internal constructor(
    vuPath: Path
) {
    private val parser = ActionMetricsParser()
    private val metrics = vuPath.resolve("action-metrics.jpt")

    /**
     * Each VU executes a scenario. Scenario contains actions. Each action can emit multiple metrics.
     * This streams these scenario metrics emitted by this VU.
     * A metric can be surrounded by another one:
     * e.g. a `SEND_MAIL` metric can contain `LOAD_EDITOR`, `FILL_FORM`, `COMPOSE_MAIL` and `SEND` metrics.
     * There can be time gaps between metrics, e.g. when the action does some processing or VU is diagnosing/throttling.
     *
     * @since 3.12.0
     */
    fun streamMetrics(): Stream<ActionMetric> {
        val stream = metrics
            .toFile()
            .inputStream()
        return parser
            .stream(stream)
            .onClose { stream.close() }
    }

    internal fun writeScenarioMetrics(): BufferedWriter {
        return metrics
            .toFile()
            .ensureParentDirectory()
            .bufferedWriter()
    }
}
