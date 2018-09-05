package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import org.apache.commons.cli.*
import java.net.URI
import java.time.Duration
import java.util.*

/**
 * Parsed cli args stored as fields.
 * Use {@link TestOptions.Parser} to parse.
 */
data class VirtualUserOptions(
    val help: Boolean = false,
    val jiraAddress: URI = URI("http://localhost:8080/"),
    val adminLogin: String = "admin",
    val adminPassword: String = "admin",
    val virtualUserLoad: VirtualUserLoad = VirtualUserLoad(),
    val scenario: Class<out Scenario> = JiraSoftwareScenario::class.java,
    val seed: Long = Random().nextLong(),
    val diagnosticsLimit: Int = 64
) {

    companion object {
        const val helpParameter = "help"
        const val jiraAddressParameter = "jira-address"
        const val loginParameter = "login"
        const val passwordParameter = "password"

        const val virtualUsersParameter = "virtual-users"
        const val holdParameter = "hold"
        const val rampParameter = "ramp"
        const val flatParameter = "flat"
        const val scenarioParameter = "scenario"
        const val seedParameter = "seed"
        const val diagnosticsLimitParameter = "diagnostics-limit"

        val options: Options = Options()
            .addOption(
                Option.builder("h")
                    .longOpt(helpParameter)
                    .desc("This help")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(jiraAddressParameter)
                    .hasArg()
                    .argName("address")
                    .desc("Address of tested JIRA as URI")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(loginParameter)
                    .hasArg()
                    .desc("Login of an admin user")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(passwordParameter)
                    .hasArg()
                    .desc("Password of an admin user")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(virtualUsersParameter)
                    .hasArg(true)
                    .desc("Number of virtual users to execute.")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(holdParameter)
                    .hasArg()
                    .desc("Initial hold duration in ISO-8601 format")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(rampParameter)
                    .hasArg()
                    .desc("Load ramp duration in ISO-8601 format")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(flatParameter)
                    .hasArg()
                    .desc("Flat load duration in ISO-8601 format")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(scenarioParameter)
                    .hasArg(true)
                    .desc("Custom scenario")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(seedParameter)
                    .hasArg(true)
                    .desc("Root seed.")
                    .required()
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(diagnosticsLimitParameter)
                    .hasArg()
                    .desc("Limiting how many times diagnostics can be executed")
                    .required()
                    .build()
            )
    }

    /**
     * Serializes to cli args.
     */
    fun toCliArgs(): Array<String> {
        val args = mutableMapOf(
            jiraAddressParameter to jiraAddress.toString(),
            loginParameter to adminLogin,
            passwordParameter to adminPassword,
            virtualUsersParameter to virtualUserLoad.virtualUsers.toString(),
            holdParameter to virtualUserLoad.hold.toString(),
            rampParameter to virtualUserLoad.ramp.toString(),
            flatParameter to virtualUserLoad.flat.toString(),
            scenarioParameter to scenario.canonicalName,
            diagnosticsLimitParameter to diagnosticsLimit.toString(),
            seedParameter to seed.toString()
        )

        val cliArgs = args.entries.flatMap { listOf("--${it.key}", it.value) }.toTypedArray()

        return if (help) {
            cliArgs + helpParameter
        } else {
            cliArgs
        }
    }

    fun printHelp() {
        HelpFormatter().printHelp("EntryPoint",
            options
        )
    }

    class Parser {

        /**
         * Parses cli args.
         */
        fun parse(args: Array<String>): VirtualUserOptions {
            val parser: CommandLineParser = DefaultParser()
            val commandLine = parser.parse(options, args)
            val help = commandLine.hasOption(helpParameter)
            val jiraAddress = URI(commandLine.getOptionValue(jiraAddressParameter))
            val adminLogin = commandLine.getOptionValue(loginParameter)
            val adminPassword = commandLine.getOptionValue(passwordParameter)
            val virtualUsers = commandLine.getOptionValue(virtualUsersParameter).toInt()
            val hold = Duration.parse(commandLine.getOptionValue(holdParameter))
            val ramp = Duration.parse(commandLine.getOptionValue(rampParameter))
            val flat = Duration.parse(commandLine.getOptionValue(flatParameter))
            val diagnosticsLimit = commandLine.getOptionValue(diagnosticsLimitParameter).toInt()
            val seed = commandLine.getOptionValue(seedParameter).toLong()

            return VirtualUserOptions(
                help = help,
                jiraAddress = jiraAddress,
                adminLogin = adminLogin,
                adminPassword = adminPassword,
                virtualUserLoad = VirtualUserLoad(
                    virtualUsers = virtualUsers,
                    hold = hold,
                    ramp = ramp,
                    flat = flat
                ),
                scenario = getScenario(commandLine),
                diagnosticsLimit = diagnosticsLimit,
                seed = seed
            )
        }

        private fun getScenario(commandLine: CommandLine): Class<out Scenario> {
            val scenario = commandLine.getOptionValue(scenarioParameter)
            val scenarioClass = Class.forName(scenario)
            val scenarioConstructor = scenarioClass.getConstructor()
            return (scenarioConstructor.newInstance() as Scenario)::class.java
        }
    }
}