package com.atlassian.performance.tools.virtualusers.measure

import net.jcip.annotations.NotThreadSafe
import net.jcip.annotations.ThreadSafe
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * Diagnoses a limited number of times.
 * Can be used to limit information overload and resource consumption.
 */
@NotThreadSafe
internal class LimitedDiagnostics(
    private val diagnostics: Diagnostics,
    private val limit: DiagnosisLimit
) : Diagnostics {

    override fun diagnose(
        exception: Exception
    ): String {
        return if (limit.isExceeded()) {
            "limit exceeded: $limit"
        } else {
            val diagnosis = diagnostics.diagnose(exception)
            limit.countDiagnosis()
            diagnosis
        }
    }
}

@ThreadSafe
class DiagnosisLimit(
    private val enoughDiagnoses: Int
) {
    private val diagnosesMade = AtomicInteger(0)
    private val logger = LogManager.getLogger(this::class.java)

    fun countDiagnosis() {
        diagnosesMade.incrementAndGet()
        if (isExceeded()) {
            logger.info("Diagnosis limit exceeded: $this")
        }
    }

    fun isExceeded(): Boolean {
        return diagnosesMade.get() >= enoughDiagnoses
    }

    override fun toString(): String = "$diagnosesMade were already made and $enoughDiagnoses diagnoses should be enough"
}