package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.virtualusers.LoadTest
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
            LogManager.getLogger(this::class.java).error("Failed to run with $args", e)
            System.exit(1)
        }
    }

    private fun run(args: Array<String>) {
        val options = VirtualUserOptions.Parser().parse(args)
        if (options.help) {
            options.printHelp()
            System.exit(0)
        }
        LoadTest(options).run()
    }
}