package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import org.apache.commons.cli.*
import java.net.URI
import java.net.URL
import java.time.Duration
import java.util.*

/**
 * Parsed cli args stored as fields.
 * Use {@link TestOptions.Parser} to parse.
 */
data class VirtualUserOptions(
    val help: Boolean,
    val jiraAddress: URI,
    val adminLogin: String,
    val adminPassword: String,
    val virtualUserLoad: VirtualUserLoad,
    val scenario: Class<out Scenario>,
    val seed: Long,
    val diagnosticsLimit: Int,
    val allowInsecureConnections: Boolean
) {
    private val normalizedJiraAddress: URI = validateJiraAddress()

    @Deprecated(
        message = "Use the primary constructor. " +
            "Kotlin defaults don't work from Java and introduce binary compatibility problems. " +
            "Moreover, forcing to think about the values exposes the powerful options at the users disposal."
    )
    constructor(
        help: Boolean = false,
        jiraAddress: URI = URI("http://localhost:8080/"),
        adminLogin: String = "admin",
        adminPassword: String = "admin",
        virtualUserLoad: VirtualUserLoad = VirtualUserLoad(),
        scenario: Class<out Scenario> = JiraSoftwareScenario::class.java,
        seed: Long = Random().nextLong(),
        diagnosticsLimit: Int = 64
    ) : this(
        help = help,
        jiraAddress = jiraAddress,
        adminLogin = adminLogin,
        adminPassword = adminPassword,
        virtualUserLoad = virtualUserLoad,
        scenario = scenario,
        seed = seed,
        diagnosticsLimit = diagnosticsLimit,
        allowInsecureConnections = false
    )

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
        const val allowInsecureConnectionsParameter = "allow-insecure-connections"

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
            .addOption(
                Option.builder()
                    .longOpt(allowInsecureConnectionsParameter)
                    .desc("Allows insecure connections to the browser")
                    .build()
            )
    }

    /**
     * Serializes to cli args.
     */
    fun toCliArgs(): Array<String> {
        val args = mutableMapOf(
            jiraAddressParameter to normalizedJiraAddress.toString(),
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

        val cliArgs = args.entries.flatMap { listOf("--${it.key}", it.value) }.toMutableList()
        if (help) {
            cliArgs.add("--$helpParameter")
        }
        if (allowInsecureConnections) {
            cliArgs.add("--$allowInsecureConnectionsParameter")
        }
        return cliArgs.toTypedArray()
    }

    private fun validateJiraAddress(): URI {
        val url = try {
            jiraAddress.toURL()
        } catch (e: Exception) {
            throw Exception("Invalid Jira URL: $jiraAddress", e)
        }
        return URL(
            url.protocol,
            url.host,
            url.port,
            when {
                url.file.endsWith("/") -> url.file
                else -> url.file + "/"
            },
            null
        ).toURI()
    }

    fun printHelp() {
        HelpFormatter().printHelp(
            "EntryPoint",
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
            val allowInsecureConnections = commandLine.hasOption(allowInsecureConnectionsParameter)

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
                seed = seed,
                allowInsecureConnections = allowInsecureConnections
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