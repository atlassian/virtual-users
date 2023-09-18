package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import net.jcip.annotations.ThreadSafe
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * TODO return interfaces/abstracts only
 * TODO ensure all types are in API
 */
@ThreadSafe
class LoadThreadContainer(
    private val processContainer: LoadProcessContainer,
    private val uuid: UUID,
    private val random: Supplier<Random>,
    private val actionMeter: Supplier<ActionMeter>,
    private val taskMeter: Supplier<ActionMeter>,
    private val threadResult: Supplier<VirtualUserResult>,
    private val singleThreadLoad: Supplier<VirtualUserLoad>,
    private val addClosableConsumer: Consumer<AutoCloseable>,
    private val onClose: Runnable
) : AutoCloseable {
    val id: String = uuid.toString()

    fun threadResult() = threadResult.get()

    fun random() = random.get()

    fun actionMeter() = actionMeter.get()

    fun taskMeter() = taskMeter.get()

    fun singleThreadLoad() = singleThreadLoad.get()

    fun loadProcessContainer() = processContainer

    fun addCloseable(closeable: AutoCloseable) {
        addClosableConsumer.accept(closeable)
    }

    override fun close() = onClose.run()

    class Builder(
        private var processContainer: LoadProcessContainer,
        private var index: Int
    ) {
        private var uuid: UUID = UUID.randomUUID()
        private var random: Supplier<Random>? = null
        private var actionMeter: Supplier<ActionMeter>? = null
        private var taskMeter: Supplier<ActionMeter>? = null
        private var threadResult: Supplier<VirtualUserResult>? = null
        private var singleThreadLoad: Supplier<VirtualUserLoad>? = null
        private var closeables: Queue<AutoCloseable> = ConcurrentLinkedQueue<AutoCloseable>()
        private var onClose: Runnable? = null

        fun uuid(uuid: UUID) = apply { this.uuid = uuid }
        fun random(random: Supplier<Random>) = apply { this.random = random }
        fun actionMeter(actionMeter: Supplier<ActionMeter>) = apply { this.actionMeter = actionMeter }
        fun taskMeter(taskMeter: Supplier<ActionMeter>) = apply { this.taskMeter = taskMeter }
        fun threadResult(threadResult: Supplier<VirtualUserResult>) = apply { this.threadResult = threadResult }
        fun singleThreadLoad(singleThreadLoad: Supplier<VirtualUserLoad>) =
            apply { this.singleThreadLoad = singleThreadLoad }

        fun closeables(closeables: Queue<AutoCloseable>) = apply { this.closeables = closeables }

        fun build(): LoadThreadContainer {
            val threadResult =
                threadResult ?: Supplier { processContainer.result().isolateVuResult(uuid.toString()) }
            return LoadThreadContainer(
                processContainer = processContainer,
                uuid = uuid,
                random = random ?: Supplier { Random(processContainer.random().nextLong()) },
                actionMeter = actionMeter ?: Supplier {
                    val actionOutput = threadResult.get().writeActionMetrics()
                    closeables.add(actionOutput)
                    ActionMeter.Builder(AppendableActionMetricOutput(actionOutput))
                        .virtualUser(uuid)
                        .build()
                },
                taskMeter = taskMeter ?: Supplier {
                    val taskOutput = threadResult.get().writeTaskMetrics()
                    closeables.add(taskOutput)
                    ActionMeter.Builder(AppendableActionMetricOutput(taskOutput))
                        .virtualUser(uuid)
                        .build()
                },
                threadResult = threadResult,
                singleThreadLoad = singleThreadLoad
                    ?: Supplier {
                        val overallLoad = processContainer.options().behavior.load
                        val singleThreadHold =
                            overallLoad.hold + overallLoad.rampInterval.multipliedBy(index.toLong())
                        VirtualUserLoad.Builder()
                            .virtualUsers(1)
                            .hold(singleThreadHold)
                            .ramp(Duration.ZERO)
                            .flat(overallLoad.total - singleThreadHold)
                            .maxOverallLoad(overallLoad.maxOverallLoad / overallLoad.virtualUsers)
                            .build()
                    },
                onClose = onClose ?: Runnable {
                    synchronized(closeables) {
                        closeables.forEach { it.close() }
                    }
                },
                addClosableConsumer = Consumer { closeables.add(it) }
            )
        }
    }
}

