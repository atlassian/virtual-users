package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior

/**
 * @since 3.13.0
 */
object VirtualUserTasks {
    /**
     * VU was performing actions. This is the main purpose of VUs.
     */
    @JvmField
    val ACTING = ActionType("VU Act") {}

    /**
     * VU was diagnosing errors. VUs should do that a little to help understand problems, but this shouldn't dominate.
     * Control it with [VirtualUserBehavior.diagnosticsLimit].
     */
    @JvmField
    val DIAGNOSING = ActionType("VU Diagnose") {}

    /**
     * VU was throttling to respect [VirtualUserLoad.maxOverallLoad].
     */
    @JvmField
    val THROTTLING = ActionType("VU Throttle") {}

    /**
     * VU was performing unknown tasks. This overhead should be close to zero.
     * It can be inferred from a full timeline minus all other tasks.
     */
    @JvmField
    val MYSTERY = ActionType("VU Mystery") {}
}
