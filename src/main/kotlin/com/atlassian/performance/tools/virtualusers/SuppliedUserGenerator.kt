package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions

internal class SuppliedUserGenerator : UserGenerator {

    override fun generateUser(
        options: VirtualUserOptions
    ): User {
        return options.target.let {
            User(it.userName, it.password)
        }
    }
}
