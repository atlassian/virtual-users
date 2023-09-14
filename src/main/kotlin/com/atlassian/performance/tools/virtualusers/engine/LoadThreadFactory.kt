package com.atlassian.performance.tools.virtualusers.engine

import com.atlassian.performance.tools.virtualusers.api.config.LoadThreadContainer

interface LoadThreadFactory {
    fun fireUp(container: LoadThreadContainer): LoadThread
}
