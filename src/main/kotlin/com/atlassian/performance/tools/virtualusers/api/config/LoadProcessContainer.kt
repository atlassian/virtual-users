package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import net.jcip.annotations.ThreadSafe
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@ThreadSafe
class LoadProcessContainer private constructor(
    private val options: VirtualUserOptions,
    private val result: VirtualUserNodeResult,
    private val seed: Long
) : AutoCloseable {

    private val closeables: Queue<AutoCloseable> = ConcurrentLinkedQueue<AutoCloseable>()

    fun result() = result
    fun options() = options
    fun random() = Random(seed)
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
            options: VirtualUserOptions,
            result: VirtualUserNodeResult,
            seed: Long
        ): LoadProcessContainer = LoadProcessContainer(options, result, seed)
    }
}
