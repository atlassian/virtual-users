package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import net.jcip.annotations.ThreadSafe
import java.util.*

@ThreadSafe
abstract class LoadProcessContainer private constructor() : AutoCloseable {

    abstract fun result(): VirtualUserNodeResult
    abstract fun options(): VirtualUserOptions
    abstract fun random(): Random
    abstract fun addCloseable(closeable: AutoCloseable)

    /**
     * TODO better name
     */
    @ThreadSafe
    internal class ConstructedLoadProcessContainer(
        private val options: VirtualUserOptions,
        private val result: VirtualUserNodeResult,
        private val seed: Long
    ) : LoadProcessContainer() {
        override fun result() = result
        override fun options() = options
        override fun random() = Random(seed)
    }
}


