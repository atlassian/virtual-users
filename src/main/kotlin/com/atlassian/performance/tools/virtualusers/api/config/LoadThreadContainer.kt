package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import net.jcip.annotations.ThreadSafe
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@ThreadSafe
class LoadThreadContainer private constructor(
    private val processContainer: LoadProcessContainer,
    private val index: Int,
    private val uuid: UUID
) : AutoCloseable {

    private val closeables: Queue<AutoCloseable> = ConcurrentLinkedQueue<AutoCloseable>()

    val id: String = uuid.toString()

    fun threadResult(): VirtualUserResult = processContainer.result().isolateVuResult(id)

    fun random(): Random {
        return Random(processContainer.random().nextLong())
    }

    fun actionMeter(): ActionMeter {
        val actionOutput = threadResult().writeActionMetrics()
        addCloseable(actionOutput)
        return ActionMeter.Builder(AppendableActionMetricOutput(actionOutput))
            .virtualUser(uuid)
            .build()
    }

    fun taskMeter(): ActionMeter {
        val taskOutput = threadResult().writeTaskMetrics()
        addCloseable(taskOutput)
        return ActionMeter.Builder(AppendableActionMetricOutput(taskOutput))
            .virtualUser(uuid)
            .build()
    }

    fun singleThreadLoad(): VirtualUserLoad {
        val overallLoad = processContainer.options().behavior.load
        val singleThreadHold = overallLoad.hold + overallLoad.rampInterval.multipliedBy(index.toLong())
        return VirtualUserLoad.Builder()
            .virtualUsers(1)
            .hold(singleThreadHold)
            .ramp(Duration.ZERO)
            .flat(overallLoad.total - singleThreadHold)
            .maxOverallLoad(overallLoad.maxOverallLoad / overallLoad.virtualUsers)
            .build()
    }

    fun loadProcessContainer() = processContainer

    fun addCloseable(closeable: AutoCloseable) {
        closeables.add(closeable)
    }

    override fun close() {
        synchronized(this) {
            closeables.forEach { it.close() }
            closeables.clear()
        }
    }

    internal companion object Factory {
        fun create(
            processContainer: LoadProcessContainer,
            index: Int,
            uuid: UUID
        ): LoadThreadContainer = LoadThreadContainer(processContainer, index, uuid)
    }
}
