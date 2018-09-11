package com.atlassian.performance.tools.virtualusers.api.diagnostics

import net.jcip.annotations.NotThreadSafe

/**
 * Diagnoses a limited number of times.
 * Can be used to limit information overload and resource consumption.
 */
@NotThreadSafe
class LimitedDiagnostics(
    private val diagnostics: Diagnostics,
    private val limit: DiagnosisLimit
) : Diagnostics {

    override fun diagnose(
        exception: Exception
    ) {
        if (limit.isExceeded().not()) {
            diagnostics.diagnose(exception)
            limit.countDiagnosis()
        }
    }
}
