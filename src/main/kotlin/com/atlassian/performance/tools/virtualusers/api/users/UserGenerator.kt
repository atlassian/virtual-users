package com.atlassian.performance.tools.virtualusers.api.users

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import net.jcip.annotations.ThreadSafe

/**
 * Supplies a user. The user can be created dynamically or it can find an existing user.
 * Expect an instance to be called concurrently.
 * If the implementation needs to be called sequentially, implement synchronization internally.
 */
@ThreadSafe
@Deprecated("Include your user generation logic in your LoadProcess")
interface UserGenerator {

    fun generateUser(options: VirtualUserOptions): User

}
