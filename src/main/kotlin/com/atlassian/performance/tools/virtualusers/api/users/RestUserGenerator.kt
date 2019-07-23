package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jvmtasks.api.TaskTimer.time
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import okhttp3.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class RestUserGenerator(readTimeout: Duration) : UserGenerator {

    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(readTimeout.seconds, TimeUnit.SECONDS)
        .build()

    constructor() : this(Duration.ofSeconds(90))

    override fun generateUser(
        options: VirtualUserOptions
    ): User {
        val target = options.target
        val uuid = UUID.randomUUID()
        val userName = "jpt-$uuid"
        val payload = """
            {
                "name": "$userName",
                "password": "${target.password}",
                "emailAddress": "$userName@testing.com",
                "displayName": "New JPT VU $userName"
            }
            """.trimIndent()
        val requestBody = RequestBody.create(MediaType.parse("application/json"), payload)
        val credential = Credentials.basic(target.userName, target.password)
        val request = Request.Builder()
            .url(target.webApplication.resolve("rest/api/2/user").toString())
            .header("Authorization", credential)
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
            if (response.code() == 201) {
                logger.info("Created a new user $userName")
            } else {
                val msg = "Failed to create a new user $userName:" +
                    " response code ${response.code()}," +
                    " response body ${response.body()?.string()}"

                logger.error(msg)
                throw Exception(msg)
            }
        }
        return User(name = userName, password = target.password)
    }
}
