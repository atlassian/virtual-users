package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import org.apache.logging.log4j.LogManager
import java.io.BufferedWriter
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class LoadSegment(
    val driver: CloseableRemoteWebDriver,
    val output: BufferedWriter,
    val done: AtomicBoolean,
    val id: UUID,
    val index: Int
) : AutoCloseable {

    override fun close() {
        done.set(true)
        output.close()
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
