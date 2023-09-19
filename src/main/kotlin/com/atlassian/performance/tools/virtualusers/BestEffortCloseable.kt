package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.concurrency.api.AbruptExecutorService
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit

/**
 * Tries to [close] the [name]d [closeable].
 * Gives up after [timeout] passes.
 * Failing to close results in a mere warning in the [logger].
 */
class BestEffortCloseable(
    private val closeable: AutoCloseable,
    private val timeout: Duration,
    private val name: String,
    private val logger: Logger
) : AutoCloseable {

    override fun close() {
        AbruptExecutorService(newSingleThreadExecutor {
            Thread(it)
                .apply { name = "try-closing-$name" }
                .apply { isDaemon = true }
        }).use { thread ->
            try {
                thread
                    .submit { closeable.close() }
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                logger.warn("Failed to close $name", e)
            }
        }
    }
}
