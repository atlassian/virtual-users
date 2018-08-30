package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.JiraSoftwareScenario
import org.apache.commons.cli.*
import java.net.URI
import java.time.Duration

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
    val scenario: Scenario = JiraSoftwareScenario(),
    val diagnosticsLimit: Int = 64
) {

    companion object {
        const val helpParameter = "help"
        const val jiraAddressParameter = "jira-address"
        const val loginParameter = "login"
        const val passwordParameter = "password"
        const val minimumRunParameter = "minimum-run"
        const val virtualUsersParameter = "virtual-users"
        const val seedParameter = "seed"
        const val rampUpIntervalParameter = "ramp-up-interval"
        const val customScenario = "scenario"
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
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(loginParameter)
                    .hasArg()
                    .desc("Login of an admin user")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(passwordParameter)
                    .hasArg()
                    .desc("Password of an admin user")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(minimumRunParameter)
                    .hasArg()
                    .desc("Minimum duration (in minutes) of virtual users activity")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(virtualUsersParameter)
                    .hasArg(true)
                    .desc("Number of virtual users to execute.")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(customScenario)
                    .hasArg(true)
                    .desc("Custom scenario")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(seedParameter)
                    .hasArg(true)
                    .desc("Root seed.")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(rampUpIntervalParameter)
                    .hasArg()
                    .desc("Interval duration between virtual users ramp-up in ISO 8601")
                    .build()
            )
            .addOption(
                Option.builder()
                    .longOpt(diagnosticsLimitParameter)
                    .hasArg()
                    .desc("Limiting how many times diagnostics can be executed")
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
//            TODO
//            minimumRunParameter to loadProfile.loadSchedule.duration minimumRun.toMinutes().toString(),
//            virtualUsersParameter to virtualUsers.toString(),
//            rampUpIntervalParameter to rampUpInterval.toString(),
            diagnosticsLimitParameter to diagnosticsLimit.toString()
        )
        if (help) {
            args[helpParameter] = ""
        }

        return args.entries.map { "--${it.key} ${it.value}" }.toTypedArray()
    }

    fun printHelp() {
        HelpFormatter().printHelp("EntryPoint", options)
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
            val timeout = Duration.ofMinutes(commandLine.getOptionValue(minimumRunParameter).toLong())
            val virtualUsers = commandLine.getOptionValue(virtualUsersParameter).toInt()
            val rampUpInterval = Duration.parse(commandLine.getOptionValue(rampUpIntervalParameter))
            val diagnosticsLimit = commandLine.getOptionValue(diagnosticsLimitParameter).toInt()
            val seed = commandLine.getOptionValue(seedParameter).toLong()

            return VirtualUserOptions(
                help = help,
                jiraAddress = jiraAddress,
                adminLogin = adminLogin,
                adminPassword = adminPassword,
//                TODO
//                minimumRun = timeout,
//                virtualUsers = virtualUsers,
//                seededRandom = SeededRandom(seed),
//                rampUpInterval = rampUpInterval,
                scenario = getScenario(commandLine),
                diagnosticsLimit = diagnosticsLimit
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