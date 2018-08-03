package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.ActionMetricStatistics
import com.atlassian.performance.tools.jiraactions.MergingActionMetricsParser
import com.atlassian.performance.tools.jiraactions.SeededRandom
import com.atlassian.performance.tools.jiraactions.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.logs.LogConfigurationFactory
import org.apache.commons.cli.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.ConfigurationFactory.setConfigurationFactory
import java.net.URI
import java.time.Duration
import java.util.*

/**
 * Parsed cli args stored as fields.
 * Use {@link TestOptions.Parser} to parse.
 */
class TestOptions(
    val help: Boolean,
    val jiraAddress: URI,
    val adminLogin: String,
    val adminPassword: String,
    val minimumRun: Duration,
    val virtualUsers: Int,
    val seededRandom: SeededRandom,
    val rampUpInterval: Duration,
    val scenario: Scenario
) {

    companion object {
        const val loginParameter = "login"
        const val passwordParameter = "password"
        const val seedParameter = "seed"
        const val rampUpInterval = "ramp-up-interval"
        const val customScenario = "scenario"

        val options: Options = Options()
            .addOption(
                Option
                    .builder("h")
                    .longOpt("help")
                    .desc("This help")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt("jira-address")
                    .hasArg()
                    .argName("address")
                    .desc("Address of tested JIRA as URI")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt(loginParameter)
                    .hasArg()
                    .desc("Login of an admin user")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt(passwordParameter)
                    .hasArg()
                    .desc("Password of an admin user")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt("minimum-run")
                    .hasArg()
                    .desc("Minimum duration (in minutes) of virtual users activity")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt("virtual-users")
                    .hasArg(true)
                    .desc("Number of virtual users to execute.")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt(customScenario)
                    .hasArg(true)
                    .desc("Custom scenario")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt(seedParameter)
                    .optionalArg(true)
                    .hasArg(true)
                    .desc("Root seed.")
                    .build()
            )
            .addOption(
                Option
                    .builder()
                    .longOpt(rampUpInterval)
                    .hasArg()
                    .desc("Interval duration between virtual users ramp-up in ISO 8601")
                    .build()
            )
    }

    fun printHelp() {
        HelpFormatter().printHelp("EntryPoint", options)
    }

    class Parser {

        /**
         * Parses cli args.
         */
        fun parse(args: Array<String>): TestOptions {
            val parser: CommandLineParser = DefaultParser()
            val commandLine = parser.parse(options, args)
            val help = commandLine.hasOption("help")
            val jiraAddress = URI(commandLine.getOptionValue("jira-address", "http://localhost:8080/"))
            val adminLogin = commandLine.getOptionValue(loginParameter, "admin")
            val adminPassword = commandLine.getOptionValue(passwordParameter, "admin")
            val timeout = Duration.ofMinutes(commandLine.getOptionValue("minimum-run", "5").toLong())
            val virtualUsers = commandLine.getOptionValue("virtual-users", "10").toInt()
            val rampUpInterval = Duration.parse(commandLine.getOptionValue(rampUpInterval, "PT0S"))

            val seed = commandLine
                .getOptionValue(seedParameter)
                ?.toLong()
                ?: Random().nextLong()

            return TestOptions(
                help = help,
                jiraAddress = jiraAddress,
                adminLogin = adminLogin,
                adminPassword = adminPassword,
                minimumRun = timeout,
                virtualUsers = virtualUsers,
                seededRandom = SeededRandom(seed),
                rampUpInterval = rampUpInterval,
                scenario = getScenario(commandLine)
            )
        }

        private fun getScenario(commandLine: CommandLine): Scenario {
            val scenario = commandLine.getOptionValue(customScenario, JiraSoftwareScenario::class.java.canonicalName)
            val scenarioClass = Class.forName(scenario) as Class<*>
            val scenarioConstructor = scenarioClass.getConstructor()
            return scenarioConstructor.newInstance() as Scenario
        }
    }
}

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
        val options = TestOptions.Parser().parse(args)
        if (options.help) {
            options.printHelp()
            System.exit(0)
        }
        val metricsFiles = BasicTest(
            jiraAddress = options.jiraAddress,
            scenario = options.scenario,
            adminLogin = options.adminLogin,
            adminPassword = options.adminPassword,
            random = options.seededRandom,
            rampUpInterval = options.rampUpInterval
        ).run(
            minimumRun = options.minimumRun,
            virtualUsers = options.virtualUsers
        )
        val metrics = MergingActionMetricsParser().parse(metricsFiles)
        val report = SimpleReport(ActionMetricStatistics(metrics)).generate()
        println(report)
    }
}