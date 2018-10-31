package com.atlassian.performance.tools.virtualusers.api.config

import java.net.URI

class VirtualUserTarget(
    internal val webApplication: URI,
    internal val userName: String,
    internal val password: String
)