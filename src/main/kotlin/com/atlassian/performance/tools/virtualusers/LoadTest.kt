package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.api.load.LoadThread
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class LoadTest(
    private val options: VirtualUserOptions
) {

    fun run(): VirtualUserNodeResult {
        val behavior = options.behavior
        val process = behavior.loadProcess.getConstructor().newInstance()
        val processContainer = LoadProcessContainer.create(
            options,
            VirtualUserNodeResult(behavior.results),
            SeededRandom(behavior.seed)
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
        val threadFactory = process.prepareFactory(processContainer)
        val threads = (1..threadCount).map { threadIndex ->
            val threadContainer = LoadThreadContainer.create(processContainer, threadIndex, UUID.randomUUID())
            val readyThread = threadFactory.prepareThread(threadContainer)
            ContainedThread(readyThread, threadContainer)
        }
        threads.forEach { thread ->
            pool.submitWithLogContext(thread.container.id) {
                sleep(load.hold.toMillis())
                thread.loadThread.generateLoad(stop)
            }
        }
        sleep(finish.toMillis())
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
