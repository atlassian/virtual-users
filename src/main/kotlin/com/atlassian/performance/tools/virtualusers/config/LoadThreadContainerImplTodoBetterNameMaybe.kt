package com.atlassian.performance.tools.virtualusers.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import net.jcip.annotations.ThreadSafe
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * TODO better name
 */
@ThreadSafe
class LoadThreadContainerImplTodoBetterNameMaybe(
    private val processContainer: LoadProcessContainer,
    private val index: Int,
    private val uuid: UUID
) : LoadThreadContainer, LoadProcessContainer by processContainer {

    private val closeables: Queue<AutoCloseable> = ConcurrentLinkedQueue<AutoCloseable>()

    override val id: String = uuid.toString()

    override fun threadResult(): VirtualUserResult = processContainer.result().isolateVuResult(id)

    override fun random(): Random {
        return Random(processContainer.random().nextLong())
    }

    override fun actionMeter(): ActionMeter {
        val actionOutput = threadResult().writeActionMetrics()
        addCloseable(actionOutput)
        return ActionMeter.Builder(AppendableActionMetricOutput(actionOutput))
            .virtualUser(uuid)
            .build()
    }

    override fun taskMeter(): ActionMeter {
        val taskOutput = threadResult().writeTaskMetrics()
        addCloseable(taskOutput)
        return ActionMeter.Builder(AppendableActionMetricOutput(taskOutput))
            .virtualUser(uuid)
            .build()
    }

    override fun singleThreadLoad(): VirtualUserLoad {
        val overallLoad = options().behavior.load
        val singleThreadHold = overallLoad.hold + overallLoad.rampInterval.multipliedBy(index.toLong())
        return VirtualUserLoad.Builder()
            .virtualUsers(1)
            .hold(singleThreadHold)
            .ramp(Duration.ZERO)
            .flat(overallLoad.total - singleThreadHold)
            .maxOverallLoad(overallLoad.maxOverallLoad / overallLoad.virtualUsers)
            .build()
    }

    override fun addCloseable(closeable: AutoCloseable) {
        closeables.add(closeable)
    }

    override fun close() {
        synchronized(this) {
            closeables.forEach { it.close() }
            closeables.clear()
        }
    }
}
