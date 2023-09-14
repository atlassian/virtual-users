package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used multiple times per [LoadProcess].
 * The thread count can come from [VirtualUserLoad.virtualUsers].
 */
interface LoadThread {
    fun generateLoad(stop: AtomicBoolean)
}
