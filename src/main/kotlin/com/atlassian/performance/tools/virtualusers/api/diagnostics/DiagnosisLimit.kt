package com.atlassian.performance.tools.virtualusers.api.diagnostics

import net.jcip.annotations.ThreadSafe
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger

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
