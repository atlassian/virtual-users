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
    private val scenarioMetrics = vuPath.resolve("action-metrics.jpt")
    private val activityMetrics = vuPath.resolve("activity.jpt")
    private val diagnoses = vuPath.resolve("diagnoses")

    /**
     * Each VU executes a scenario. Scenario contains actions. Each action can emit multiple metrics.
     * This streams these scenario metrics emitted by this VU.
     * A metric can be surrounded by another one:
     * e.g. a `SEND_MAIL` metric can contain `LOAD_EDITOR`, `FILL_FORM`, `COMPOSE_MAIL` and `SEND` metrics.
     * There can be time gaps between metrics, e.g. when the action does some processing or VU is diagnosing/throttling.
     *
     * @since 3.12.0
     */
    fun streamScenarioMetrics(): Stream<ActionMetric> = stream(scenarioMetrics)

    /**
     * Each VU has performs one activity at a time, e.g scenario actions, diagnosing, throttling.
     * They should not overlap on a timeline.
     *
     * @since 3.12.0
     */
    fun streamActivityMetrics(): Stream<ActionMetric> = stream(activityMetrics)

    private fun stream(
        metrics: Path
    ): Stream<ActionMetric> {
        val file = metrics.toFile()
        if (file.exists().not()) {
            return Stream.empty()
        }
        val stream = file.inputStream()
        return parser
            .stream(stream)
            .onClose { stream.close() }
    }

    internal fun writeScenarioMetrics(): BufferedWriter = write(scenarioMetrics)

    internal fun writeActivityMetrics(): BufferedWriter = write(activityMetrics)

    private fun write(
        metrics: Path
    ): BufferedWriter = metrics
        .toFile()
        .ensureParentDirectory()
        .bufferedWriter()

    /**
     * Points to the directory with diagnoses. The directory might not exist.
     * If it exists, it contains all diagnoses made by the virtual user.
     *
     * @since 3.12.0
     */
    fun getDiagnoses(): Path = diagnoses
}
