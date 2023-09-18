package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.measure.output.AppendableActionMetricOutput
import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainerDefaults
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.engine.LoadThread
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

internal class LoadTest(
    private val options: VirtualUserOptions
) {
    private val behavior = options.behavior
    private val process = behavior.loadProcess.getConstructor().newInstance()

    fun run(): VirtualUserNodeResult {
        val processContainer = LoadProcessContainer.ConstructedLoadProcessContainer(
            options,
            VirtualUserNodeResult(behavior.results),
            ClusterNodeCounter(),
            behavior.seed
        )
        val load = behavior.load
        val finish = load.ramp + load.flat
        val threadCount = load.virtualUsers
        val threadCounter = AtomicInteger(0)
        val pool = ThreadPoolExecutor(
            threadCount,
            threadCount,
            0L,
            MILLISECONDS,
            LinkedBlockingQueue<Runnable>()
        ) { runnable ->
            val name = "virtual-user-%d".format(threadCounter.incrementAndGet())
            Thread(runnable, name).apply { isDaemon = true }
        }
        val stop = AtomicBoolean(false)
        val threadFactory = process.setUp(processContainer)
        val threads = (1..threadCount).map { threadIndex ->
            val defaults = LoadThreadContainerDefaults(processContainer, threadIndex, UUID.randomUUID())
            val threadContainer = LoadThreadContainer.Builder(defaults)
                .actionMeter(Supplier {
                    val actionOutput = defaults.threadResult().writeActionMetrics()
                    defaults.addCloseable(actionOutput)
                    ActionMeter.Builder(AppendableActionMetricOutput(actionOutput))
                        .virtualUser(defaults.uuid)
                        .build()
                })
                .build()
            val readyThread = threadFactory.fireUp(threadContainer)
            ContainedThread(readyThread, threadContainer)
        }
        threads.forEach { engine ->
            pool.submitWithLogContext(engine.container.id()) {
                engine.loadThread.generateLoad(stop)
            }
        }
        Thread.sleep(finish.toMillis())
        stop.set(true)
        processContainer.result().writeNodeCounts().use { processContainer.nodeCounter().dump(it) }
        threads.forEach {
            it.container.close()
        }
        return processContainer.result()
    }
}

private class ContainedThread(
    val loadThread: LoadThread,
    val container: LoadThreadContainer
)
