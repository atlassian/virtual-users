package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.virtualusers.LoadTest
import com.atlassian.performance.tools.virtualusers.RestUserGenerator
import com.atlassian.performance.tools.virtualusers.logs.LogConfigurationFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.ConfigurationFactory.setConfigurationFactory

/**
 * Main entry point for the virtual users load test.
 *
 * [args] command line args. Use [VirtualUserOptions.toCliArgs] to provide the args.
 */
fun main(args: Array<String>) {
    Application().tryRunning(args)
}

class Application {
    fun tryRunning(args: Array<String>) {
        setConfigurationFactory(LogConfigurationFactory())
        try {
            run(args)
        } catch (e: Exception) {
            val errorMessage = "Failed to run with ${args.toList()}"
            LogManager.getLogger(this::class.java).error(errorMessage)
            throw Exception(errorMessage, e)
        }
    }

    private fun run(args: Array<String>) {
        val options = VirtualUserOptions.Parser().parse(args)
        @Suppress("DEPRECATION")
        if (options.help) {
            options.printHelp()
        } else {
            LoadTest(options, RestUserGenerator(options.target)).run()
        }
    }
}