package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionType

class VirtualUserActivity internal constructor(
    val type: ActivityType,
    val metric: ActionMetric
)

enum class ActivityType(
    internal val actionType: ActionType<*>
) {
    ACTING(ActionType<Unit>("VU Act") {}),
    DIAGNOSING(ActionType<Unit>("VU Diagnose") {}),
    THROTTLING(ActionType<Unit>("VU Throttle") {}),
    MYSTERY(ActionType<Unit>("VU Mystery") {})
}
