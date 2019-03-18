package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import okhttp3.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.TimeUnit

internal class RestUserGenerator(
    private val target: VirtualUserTarget
) : UserGenerator {

    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    override fun generateUsers(userCount: Int): List<User> {
        val uuid = UUID.randomUUID()
        return (1..userCount).map { i -> createUser(i, uuid) }
    }

    private fun createUser(i: Int, uuid: UUID): User {
        val userName = "jpt$i-$uuid"
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
        val response = httpClient.newCall(request).execute()

        response.use {
            if (response.code() == 201) {
                logger.info("Created a new user $userName")
            } else {
                throw Exception(
                    "Failed to create a new user $userName:" +
                        " response code ${response.code()}," +
                        " response body ${response.body()?.string()}"
                )
            }
        }
        return User(name = userName, password = target.password)
    }
}
