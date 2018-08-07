package com.atlassian.performance.tools.virtualusers.logs

import org.apache.logging.log4j.Level.DEBUG
import org.apache.logging.log4j.core.config.AbstractConfiguration
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout

class LogConfiguration : AbstractConfiguration(null, ConfigurationSource.NULL_SOURCE) {
    override fun doConfigure() {
        val layout = PatternLayout.newBuilder()
            .withPattern("%d{ISO8601}{UTC}Z %-5level %X %x [%logger{1}] %msg%n")
            .withConfiguration(this)
            .build()

        val loggerConfig = LoggerConfig("com.atlassian.performance.tools", DEBUG, false)
        val fileAppender = FileAppenderBuilderWrapper()
            .create(
                "file",
                "virtual-users.log",
                false,
                layout
            )
        loggerConfig.addAppender(
            fileAppender,
            DEBUG,
            null
        )
        this.addLogger(loggerConfig.name, loggerConfig)
    }
}