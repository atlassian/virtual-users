package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.engine.LoadThread
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class LoadTest(
    private val options: VirtualUserOptions
) {
    private val behavior = options.behavior
    private val process = behavior.loadProcess.getConstructor().newInstance()

    fun run(): VirtualUserNodeResult {
        val processContainer = LoadProcessContainer.ConstructedLoadProcessContainer(
            options,
            VirtualUserNodeResult(behavior.results),
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
            val threadContainer = LoadThreadContainer.create(processContainer, threadIndex, UUID.randomUUID())
            val readyThread = threadFactory.fireUp(threadContainer)
            ContainedThread(readyThread, threadContainer)
        }
        threads.forEach { engine ->
            pool.submitWithLogContext(engine.container.id) {
                engine.loadThread.generateLoad(stop)
            }
        }
        Thread.sleep(finish.toMillis())
        stop.set(true)
        processContainer.close()
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
