package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.memories.User

interface UserGenerator {

	fun generateUsers(userCount: Int): List<User>

}
