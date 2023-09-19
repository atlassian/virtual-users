package com.atlassian.performance.tools.virtualusers.api.load

import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import net.jcip.annotations.NotThreadSafe
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All [LoadThread]s are thread-confined.
 */
@NotThreadSafe
interface LoadThread {

    /**
     * Generate load until [stop].
     * Consider using an injected [ActionMeter] to record measurements.
     */
    fun generateLoad(stop: AtomicBoolean)
}
