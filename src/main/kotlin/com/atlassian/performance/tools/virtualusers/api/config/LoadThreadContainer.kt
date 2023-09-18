package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import net.jcip.annotations.ThreadSafe
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * TODO split the interface
 * TODO return interfaces/abstracts only
 * TODO ensure all types are in API
 */
@ThreadSafe
abstract class LoadThreadContainer private constructor() : AutoCloseable {

    abstract val id: String
    abstract fun actionMeter(): ActionMeter
    abstract fun taskMeter(): ActionMeter
    abstract fun threadResult(): VirtualUserResult
    abstract fun addCloseable(closeable: AutoCloseable)
    abstract fun singleThreadLoad(): VirtualUserLoad
    abstract fun loadProcessContainer(): LoadProcessContainer
    abstract fun random(): Random

    /**
     * TODO better name
     */
    @ThreadSafe
    internal class LoadThreadContainerImplTodoBetterNameMaybe(
        private val processContainer: LoadProcessContainer,
        private val index: Int,
        private val uuid: UUID
    ) : LoadThreadContainer() {

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

        override fun loadProcessContainer() = processContainer

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
}

