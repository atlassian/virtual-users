package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.AbruptExecutorService
import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import com.atlassian.performance.tools.virtualusers.api.load.LoadThread
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class LoadTest(
    private val options: VirtualUserOptions
) {

    private val logger: Logger = LogManager.getLogger(this::class.java)

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
        close(threads)
        processContainer.close()
        return processContainer.result()
    }

    private fun close(threads: List<ContainedThread>) {
        logger.info("Closing thread containers")
        AbruptExecutorService(newCachedThreadPool { Thread(it, "close-thread-containers") }).use { pool ->
            threads
                .map { pool.submit { it.container.close() } }
                .forEach { it.get() }
        }
        logger.info("Thread containers closed")
    }
}

private class ContainedThread(
    val loadThread: LoadThread,
    val container: LoadThreadContainer
)
