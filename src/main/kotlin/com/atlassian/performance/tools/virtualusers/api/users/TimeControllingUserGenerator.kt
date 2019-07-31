package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.Instant.now
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Ensures that the user generation will take [targetTime].
 * It's highly recommended to use it in order to get a predictable load schedule.
 *
 * @since 3.10.0
 */
class TimeControllingUserGenerator(
    private val targetTime: Duration,
    private val userGenerator: UserGenerator
) : UserGenerator {

    private val logger: Logger = LogManager.getLogger(this::class.java)

    override fun generateUser(
        options: VirtualUserOptions
    ): User {
        val thread = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "time-controlled-user-generation-${runnable.hashCode()}")
        }
        try {
            return controlGenerationTime(thread, options)
        } finally {
            thread.shutdownNow()
        }
    }

    private fun controlGenerationTime(
        thread: ExecutorService,
        options: VirtualUserOptions
    ): User {
        val start = now()
        val futureUser = thread.submit(Callable {
            userGenerator.generateUser(options)
        })
        val user = futureUser.get(targetTime.seconds, TimeUnit.SECONDS)
        val elapsed = Duration.between(start, now())
        val remaining = targetTime - elapsed
        if (remaining > Duration.ZERO) {
            logger.info("User generated in time, with $remaining leeway")
            Thread.sleep(remaining.toMillis())
        } else {
            throw Exception("We should have finished by now, we're $remaining late")
        }
        return user
    }
}
