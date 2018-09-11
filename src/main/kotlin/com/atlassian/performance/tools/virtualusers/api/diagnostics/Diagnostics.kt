package com.atlassian.performance.tools.virtualusers.api.diagnostics

interface Diagnostics {

    fun diagnose(exception: Exception)
}
