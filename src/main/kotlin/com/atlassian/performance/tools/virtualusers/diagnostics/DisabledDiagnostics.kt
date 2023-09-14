package com.atlassian.performance.tools.virtualusers.diagnostics

import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics

class DisabledDiagnostics : Diagnostics {
    override fun diagnose(exception: Exception): Unit = Unit
}
