package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import org.apache.logging.log4j.LogManager
import java.io.BufferedWriter
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class LoadSegment(
    val driver: CloseableRemoteWebDriver,
    val actionOutput: BufferedWriter,
    val taskOutput: BufferedWriter,
    val diagnostics: Diagnostics,
    val user: User,
    val done: AtomicBoolean,
    val id: UUID,
    val index: Int
) : AutoCloseable {

    override fun close() {
        done.set(true)
        actionOutput.close()
        taskOutput.close()
        val executor = Executors.newSingleThreadExecutor {
            Thread(it)
                .apply { name = "close-driver" }
                .apply { isDaemon = true }
        }
        try {
            executor
                .submit { driver.close() }
                .get(DRIVER_CLOSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            LOGGER.warn("Failed to close WebDriver", e)
        }
    }

    internal companion object {
        private val LOGGER = LogManager.getLogger(this::class.java)
        internal val DRIVER_CLOSE_TIMEOUT = Duration.ofSeconds(30)
    }
}
