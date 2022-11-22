package com.atlassian.performance.tools.virtualusers.api.browsers

import com.atlassian.performance.tools.virtualusers.api.VirtualUserResult

interface Browser {
    fun start(): CloseableRemoteWebDriver

    @JvmDefault
    fun start(vuResult: VirtualUserResult): CloseableRemoteWebDriver {
        return start()
    }
}
