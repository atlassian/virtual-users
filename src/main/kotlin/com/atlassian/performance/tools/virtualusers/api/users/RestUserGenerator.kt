package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.data.utils.UserNameUtils
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jvmtasks.api.ExponentialBackoff
import com.atlassian.performance.tools.jvmtasks.api.IdempotentAction
import com.atlassian.performance.tools.jvmtasks.api.TaskTimer.time
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import okhttp3.*
import org.apache.http.entity.ContentType.APPLICATION_JSON
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.HttpURLConnection
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Generates Jira users via its REST API.
 * Since 3.11.0, generates realistic user names.
 * @since 3.6.0
 */
class RestUserGenerator(readTimeout: Duration) : UserGenerator {
    private val nameGenerator = UserNameGenerator()

    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(readTimeout.seconds, TimeUnit.SECONDS)
        .build()

    constructor() : this(Duration.ofSeconds(90))

    override fun generateUser(
        options: VirtualUserOptions
    ): User {
        val target = options.target

        val credential = Credentials.basic(target.userName, target.password)
        val reqBuilder = Request.Builder()
            .url(target.webApplication.resolve("rest/api/2/user").toString())
            .header("Authorization", credential)
        val userName = IdempotentAction("create user via REST") {
            createUser(reqBuilder, target.password)
        }.retry(5, ExponentialBackoff(Duration.ofSeconds(1)))

        return User(name = userName, password = target.password)
    }

    private fun createUser(reqBuilder: Request.Builder, password: String) : String {
        val givenNamesUsedInThisAttempt = nameGenerator.getNumberOfGivenNamesToGenerate()
        val (userName, payload) = createUserPayload(password, givenNamesUsedInThisAttempt)
        val requestBody = RequestBody.create(MediaType.parse(APPLICATION_JSON.toString()), payload)
        val request = reqBuilder
            .post(requestBody)
            .build()

        time("create user via REST") {
            try {
                httpClient.newCall(request).execute()
            } catch (e: Exception) {
                logger.error("Could not create user $userName", e)
                throw e
            }
        }.use { response ->
            if (response.code() == HttpURLConnection.HTTP_CREATED) {
                nameGenerator.onSuccess()
                logger.info("Created a new user $userName")
                return userName
            } else {
                val possibleReason = if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    nameGenerator.onCollision(givenNamesUsedInThisAttempt)
                    "assuming name collision"
                } else {
                    "unknown reason"
                }
                val msg = "Failed to create a new user $userName, " +
                    possibleReason + ": " +
                    " response code ${response.code()}," +
                    " response body [${response.body()?.string()}]" + response.headers()

                logger.error(msg)
                throw Exception(msg)
            }
        }
    }

    private fun createUserPayload(password: String, givenNamesCnt: Int): Pair<String, String> {
        val displayName = nameGenerator.pickRandomUnique(givenNamesCnt)
        val userName = UserNameUtils.toUserName(displayName)
        val payload = """
                {
                    "name": "$userName",
                    "password": "$password",
                    "emailAddress": "$userName@jpt-testing.com",
                    "displayName": "$displayName"
                }
                """.trimIndent()
        return Pair(userName, payload)
    }
}
