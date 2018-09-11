package com.atlassian.performance.tools.virtualusers.api.diagnostics

import net.jcip.annotations.ThreadSafe
import org.apache.logging.log4j.LogManager
import java.time.Duration

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
