package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer
import net.jcip.annotations.ThreadSafe

/**
 * Used multiple times per [LoadProcess].
 */
@ThreadSafe
interface LoadThreadFactory {
    fun prepareThread(container: LoadThreadContainer): LoadThread
}
