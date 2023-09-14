package com.atlassian.performance.tools.virtualusers.config

import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import net.jcip.annotations.ThreadSafe
import java.util.*

/**
 * TODO better name
 */
@ThreadSafe
internal class ConstructedLoadProcessContainer(
    private val options: VirtualUserOptions,
    private val result: VirtualUserNodeResult,
    private val nodeCounter: ClusterNodeCounter,
    private val seed: Long
) : LoadProcessContainer {
    override fun result() = result
    override fun options() = options
    override fun random() = Random(seed)
    override fun nodeCounter() = nodeCounter
}
