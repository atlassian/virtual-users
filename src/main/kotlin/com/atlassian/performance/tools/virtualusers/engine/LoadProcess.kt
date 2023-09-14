package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer

/**
 * Used once per VU JVM.
 * Implementations must have a no-arg constructor.
 */
interface LoadProcess {
    fun setUp(container: LoadProcessContainer): LoadThreadFactory
}
