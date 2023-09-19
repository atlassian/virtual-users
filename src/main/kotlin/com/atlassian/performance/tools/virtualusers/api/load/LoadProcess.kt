package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import net.jcip.annotations.NotThreadSafe

/**
 * Used once per VU JVM.
 * Implementations must have a no-arg constructor.
 */
@NotThreadSafe
interface LoadProcess {
    fun prepareFactory(container: LoadProcessContainer): LoadThreadFactory
}
