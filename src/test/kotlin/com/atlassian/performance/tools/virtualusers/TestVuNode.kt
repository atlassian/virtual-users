package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.io.api.resolveSafely
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

internal object TestVuNode {

    fun isolateTestNode(testClass: Class<*>): Path = Paths.get("build")
        .resolve("vu-nodes")
        .resolve(testClass.simpleName)
        .resolveSafely(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME))
}
