package com.atlassian.performance.tools.virtualusers.measure

import net.jcip.annotations.NotThreadSafe
import net.jcip.annotations.ThreadSafe
import org.apache.logging.log4j.LogManager
import java.time.Duration
import java.time.Instant.now

/**
 * Diagnoses as long as no diagnosis exhausts [patience].
 */
@NotThreadSafe
internal class ImpatientDiagnostics(
    private val diagnostics: Diagnostics,
    private val patience: DiagnosisPatience
) : Diagnostics {

    override fun diagnose(
        exception: Exception
    ): String {
        if (patience.lost) {
            return "refused due to lost patience ($patience)"
        }
        val start = now()
        try {
            return diagnostics.diagnose(exception)
        } finally {
            patience.test(Duration.between(start, now()))
        }
    }
}

@ThreadSafe
class DiagnosisPatience(
    private val patience: Duration
) {
    @Volatile
    var lost = false
        private set
    private val logger = LogManager.getLogger(this::class.java)

    fun test(
        duration: Duration
    ) {
        if (duration > patience) {
            lost = true
            logger.info("Lost patience after waiting $duration for diagnosis, no more diagnoses will be made")
        }
    }
}