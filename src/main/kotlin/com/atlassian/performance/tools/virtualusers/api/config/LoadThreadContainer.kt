package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult
import net.jcip.annotations.ThreadSafe

/**
 * TODO split the interface
 * TODO return interfaces/abstracts only
 * TODO ensure all types are in API
 */
@ThreadSafe
interface LoadThreadContainer : LoadProcessContainer, AutoCloseable {

    val id: String
    fun actionMeter(): ActionMeter
    fun taskMeter(): ActionMeter
    fun threadResult(): VirtualUserResult
    fun addCloseable(closeable: AutoCloseable)
    fun singleThreadLoad(): VirtualUserLoad
}

