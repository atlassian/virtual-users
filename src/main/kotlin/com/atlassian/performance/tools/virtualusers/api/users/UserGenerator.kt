package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import net.jcip.annotations.ThreadSafe

@ThreadSafe
interface UserGenerator {

    fun generateUser(options: VirtualUserOptions): User

}
