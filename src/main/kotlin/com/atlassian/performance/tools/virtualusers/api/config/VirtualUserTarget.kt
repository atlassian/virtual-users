package com.atlassian.performance.tools.virtualusers.api.config

import java.net.URI

class VirtualUserTarget(
    val webApplication: URI,
    val userName: String,
    val password: String
)
