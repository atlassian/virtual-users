package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions

class SuppliedUserGenerator : UserGenerator {

    override fun generateUser(
        options: VirtualUserOptions
    ): User {
        return options.target.let {
            User(it.userName, it.password)
        }
    }
}
