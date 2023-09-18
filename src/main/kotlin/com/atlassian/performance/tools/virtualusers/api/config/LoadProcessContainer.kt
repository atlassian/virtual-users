package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import net.jcip.annotations.ThreadSafe
import java.util.*

/**
 * TODO ensure all types are in API
 */
@ThreadSafe
abstract class LoadProcessContainer private constructor() {

    abstract fun result(): VirtualUserNodeResult
    abstract fun options(): VirtualUserOptions
    abstract fun random(): Random
    abstract fun nodeCounter(): ClusterNodeCounter

    /**
     * TODO better name
     */
    @ThreadSafe
    internal class ConstructedLoadProcessContainer(
        private val options: VirtualUserOptions,
        private val result: VirtualUserNodeResult,
        private val nodeCounter: ClusterNodeCounter,
        private val seed: Long
    ) : LoadProcessContainer() {
        override fun result() = result
        override fun options() = options
        override fun random() = Random(seed)
        override fun nodeCounter() = nodeCounter
    }
}


