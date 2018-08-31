package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.logs.LogConfigurationFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.ConfigurationFactory.setConfigurationFactory

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
        BasicTest(options).run()
    }
}