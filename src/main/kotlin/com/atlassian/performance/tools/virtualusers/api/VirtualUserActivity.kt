package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior

/**
 * @since 3.12.0
 */
class VirtualUserActivity internal constructor(
    val type: ActivityType,
    val metric: ActionMetric
)

/**
 * @since 3.12.0
 */
enum class ActivityType(
    internal val actionType: ActionType<*>
) {
    /**
     * VU was performing actions. This is the main purpose of VUs.
     */
    ACTING(ActionType<Unit>("VU Act") {}),

    /**
     * VU was diagnosing errors. VUs should do that a little to help understand problems, but this shouldn't dominate.
     * Control it with [VirtualUserBehavior.diagnosticsLimit].
     */
    DIAGNOSING(ActionType<Unit>("VU Diagnose") {}),

    /**
     * VU was throttling to respect [VirtualUserLoad.maxOverallLoad].
     */
    THROTTLING(ActionType<Unit>("VU Throttle") {}),

    /**
     * VU was performing unknown activities. This overhead should be close to zero.
     * It can be inferred from a full timeline minus all other activities.
     */
    MYSTERY(ActionType<Unit>("VU Mystery") {})
}
