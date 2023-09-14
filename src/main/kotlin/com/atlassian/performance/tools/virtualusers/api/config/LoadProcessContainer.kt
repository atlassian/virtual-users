package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.virtualusers.api.VirtualUserNodeResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.measure.ClusterNodeCounter
import net.jcip.annotations.ThreadSafe
import java.util.*

/**
 * TODO split the interface
 * TODO return interfaces/abstracts only
 * TODO ensure all types are in API
 */
@ThreadSafe
interface LoadProcessContainer {

    fun result(): VirtualUserNodeResult
    fun options(): VirtualUserOptions
    fun random(): Random
    fun nodeCounter(): ClusterNodeCounter
}

