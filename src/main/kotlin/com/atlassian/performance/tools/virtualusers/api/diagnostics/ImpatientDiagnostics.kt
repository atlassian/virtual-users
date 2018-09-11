package com.atlassian.performance.tools.virtualusers.api.diagnostics

import net.jcip.annotations.NotThreadSafe
import java.time.Duration
import java.time.Instant.now

/**
 * Diagnoses as long as no diagnosis exhausts [patience].
 */
@NotThreadSafe
class ImpatientDiagnostics(
    private val diagnostics: Diagnostics,
    private val patience: DiagnosisPatience
) : Diagnostics {

    override fun diagnose(
        exception: Exception
    ) {
        if (patience.lost) {
            return
        }
        val start = now()
        try {
            return diagnostics.diagnose(exception)
        } finally {
            patience.test(Duration.between(start, now()))
        }
    }
}
