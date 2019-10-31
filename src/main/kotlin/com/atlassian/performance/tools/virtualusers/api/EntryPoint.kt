package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.virtualusers.LoadTest
import com.atlassian.performance.tools.virtualusers.NewLoadTest
import com.atlassian.performance.tools.virtualusers.logs.LogConfiguration
import com.atlassian.performance.tools.virtualusers.logs.LogConfigurationFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.AbstractConfiguration
import org.apache.logging.log4j.core.config.ConfigurationFactory.setConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource

/**
 * Main entry point for the virtual users load test.
 *
 * [args] command line args. Use [VirtualUserOptions.toCliArgs] to provide the args.
 */
fun main(args: Array<String>) {
    Application().tryRunning(args)
}

class Application {
    private val defaultLogConfigFactory = LogConfigurationFactory(LogConfiguration())

    fun tryRunning(args: Array<String>) {
        setConfigurationFactory(defaultLogConfigFactory)
        try {
            run(args)
        } catch (e: Exception) {
            val errorMessage = "Failed to run with ${args.toList()}"
            LogManager.getLogger(this::class.java).error(errorMessage)
            throw Exception(errorMessage, e)
        }
    }

    private fun run(args: Array<String>) {
        val options: VirtualUserOptions = VirtualUserOptions.Parser().parse(args)

        setLoggingConfiguration(options)

        @Suppress("DEPRECATION")
        if (options.help) {
            options.printHelp()
        } else {
            val scenarioPackage = options.behavior.scenario.packageName
            if (scenarioPackage == "com.atlassian.performance.tools.virtualusers.lib.api") {
                NewLoadTest(options).run()
            } else if (scenarioPackage == "com.atlassian.performance.tools.jiraactions.api.scenario") {
                LoadTest(options).run()
            } else {
                throw Exception("not implemented")
            }
        }
    }

    private fun setLoggingConfiguration(options: VirtualUserOptions) {
        val logging = options.behavior.logging

        val logConfig = logging.getConstructor().newInstance() as AbstractConfiguration
        val logConfigurationFactory = LogConfigurationFactory(logConfig)
        setConfigurationFactory(logConfigurationFactory)
        val context = LogManager.getContext(false) as LoggerContext
        context.start(logConfigurationFactory.getConfiguration(null, ConfigurationSource.NULL_SOURCE))
    }
}
