package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import org.apache.commons.cli.*
import java.net.URI
import java.net.URL
import java.time.Duration
import java.util.*

/**
 * Parsed cli args stored as fields.
 * Use {@link TestOptions.Parser} to parse.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
class VirtualUserOptions(
    val target: VirtualUserTarget,
    val behavior: VirtualUserBehavior
) {
    @Deprecated(deprecatedGetterMessage)
    @Suppress("DEPRECATION")
    val help: Boolean
        get() = behavior.help

    @Deprecated(deprecatedGetterMessage)
    val jiraAddress: URI
        get() = target.webApplication

    @Deprecated(deprecatedGetterMessage)
    val adminLogin: String
        get() = target.userName

    @Deprecated(deprecatedGetterMessage)
    val adminPassword: String
        get() = target.password

    @Deprecated("This field moved to VirtualUserBehavior", ReplaceWith("behavior.load"))
    val virtualUserLoad: VirtualUserLoad
        get() = behavior.load

    @Deprecated(deprecatedGetterMessage)
    val scenario: Class<out Scenario>
        get() = behavior.scenario

    @Deprecated(deprecatedGetterMessage)
    val seed: Long
        get() = behavior.seed

    @Deprecated(deprecatedGetterMessage)
    val diagnosticsLimit: Int
        get() = behavior.diagnosticsLimit

    @Deprecated(deprecatedGetterMessage)
    val browser: Class<out Browser>
        get() = behavior.browser

    private val normalizedJiraAddress: URI = validateJiraAddress()

    @Deprecated(
        message = "Use the 2-arg constructor. " +
            "Kotlin defaults don't work from Java and introduce binary compatibility problems. " +
            "Moreover, forcing to think about the values exposes the powerful options at the users disposal."
    )
    @Suppress("DEPRECATION")
    constructor(
        help: Boolean = false,
        jiraAddress: URI = URI("http://localhost:8080/"),
        adminLogin: String = "admin",
        adminPassword: String = "admin",
        virtualUserLoad: VirtualUserLoad = VirtualUserLoad(),
        scenario: Class<out Scenario> = JiraSoftwareScenario::class.java,
        seed: Long = Random().nextLong(),
        diagnosticsLimit: Int = 64,
        allowInsecureConnections: Boolean = false
    ) : this(
        help = help,
        jiraAddress = jiraAddress,
        adminLogin = adminLogin,
        adminPassword = adminPassword,
        virtualUserLoad = virtualUserLoad,
        scenario = scenario,
        seed = seed,
        diagnosticsLimit = diagnosticsLimit,
        browser = if (allowInsecureConnections) {
            com.atlassian.performance.tools.virtualusers.GoogleChromeWithInsecureConnectionSupport::class.java
        } else {
            HeadlessChromeBrowser::class.java
        }
    )

    @Deprecated(message = "Use the 2-arg constructor")
    @Suppress("DEPRECATION")
    constructor(
        @Suppress("UNUSED_PARAMETER") help: Boolean,
        jiraAddress: URI,
        adminLogin: String,
        adminPassword: String,
        virtualUserLoad: VirtualUserLoad,
        scenario: Class<out Scenario>,
        seed: Long,
        diagnosticsLimit: Int,
        browser: Class<out Browser>
    ) : this(
        target = VirtualUserTarget(
            webApplication = jiraAddress,
            userName = adminLogin,
            password = adminPassword
        ),
        behavior = VirtualUserBehavior.Builder(scenario)
            .load(virtualUserLoad)
            .seed(seed)
            .diagnosticsLimit(diagnosticsLimit)
            .browser(browser)
            .build()
    )

    fun withTarget(
        target: VirtualUserTarget
    ) = VirtualUserOptions(
        target = target,
        behavior = behavior
    )

    fun withBehavior(
        behavior: VirtualUserBehavior
    ) = VirtualUserOptions(
        target = target,
        behavior = behavior
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
        const val browserParameter = "browser"
        const val seedParameter = "seed"
        const val diagnosticsLimitParameter = "diagnostics-limit"
        const val allowInsecureConnectionsParameter = "allow-insecure-connections"
        const val skipSetupParameter = "skip-setup"

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
                    .longOpt(browserParameter)
                    .hasArg(true)
                    .desc("Custom browser")
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
            .addOption(
                Option.builder()
                    .longOpt(skipSetupParameter)
                    .desc("Skips the setup action")
                    .build()
            )
    }

    @Deprecated(
        message = "You can configure browser options by implementing Browser SPI"
    )
    @Suppress("DeprecatedCallableAddReplaceWith")
    fun getAllowInsecureConnections(): Boolean {
        @Suppress("DEPRECATION")
        return behavior.browser == com.atlassian.performance.tools.virtualusers.GoogleChromeWithInsecureConnectionSupport::class.java
    }

    /**
     * Serializes to CLI args.
     */
    fun toCliArgs(): Array<String> {
        @Suppress("DEPRECATION")
        val flags: List<String> = mapOf(
            helpParameter to behavior.help,
            allowInsecureConnectionsParameter to getAllowInsecureConnections(),
            skipSetupParameter to behavior.skipSetup
        ).mapNotNull { (parameter, value) ->
            if (value) "--$parameter" else null
        }
        val parameters: List<String> = mapOf(
            jiraAddressParameter to normalizedJiraAddress,
            loginParameter to target.userName,
            passwordParameter to target.password,
            virtualUsersParameter to behavior.load.virtualUsers,
            holdParameter to behavior.load.hold,
            rampParameter to behavior.load.ramp,
            flatParameter to behavior.load.flat,
            scenarioParameter to behavior.scenario.canonicalName,
            diagnosticsLimitParameter to behavior.diagnosticsLimit,
            seedParameter to behavior.seed,
            browserParameter to behavior.browser.name
        ).flatMap { (parameter, value) ->
            listOf("--$parameter", value.toString())
        }
        return (flags + parameters).toTypedArray()
    }

    private fun validateJiraAddress(): URI {
        val url = try {
            target.webApplication.toURL()
        } catch (e: Exception) {
            throw Exception("Invalid Jira URL: ${target.webApplication}", e)
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
            val jiraAddress = URI(commandLine.getOptionValue(jiraAddressParameter))
            val adminLogin = commandLine.getOptionValue(loginParameter)
            val adminPassword = commandLine.getOptionValue(passwordParameter)
            val virtualUsers = commandLine.getOptionValue(virtualUsersParameter).toInt()
            val hold = Duration.parse(commandLine.getOptionValue(holdParameter))
            val ramp = Duration.parse(commandLine.getOptionValue(rampParameter))
            val flat = Duration.parse(commandLine.getOptionValue(flatParameter))
            val diagnosticsLimit = commandLine.getOptionValue(diagnosticsLimitParameter).toInt()
            val seed = commandLine.getOptionValue(seedParameter).toLong()
            val skipSetup = commandLine.hasOption(skipSetupParameter)

            return VirtualUserOptions(
                target = VirtualUserTarget(
                    webApplication = jiraAddress,
                    userName = adminLogin,
                    password = adminPassword
                ),
                behavior = VirtualUserBehavior.Builder(getScenario(commandLine))
                    .diagnosticsLimit(diagnosticsLimit)
                    .seed(seed)
                    .browser(getBrowser(commandLine))
                    .load(VirtualUserLoad(
                        virtualUsers = virtualUsers,
                        hold = hold,
                        ramp = ramp,
                        flat = flat
                    ))
                    .skipSetup(skipSetup)
                    .build()
            )
        }

        private fun getScenario(commandLine: CommandLine): Class<out Scenario> {
            val scenario = commandLine.getOptionValue(scenarioParameter)
            val scenarioClass = Class.forName(scenario)
            val scenarioConstructor = scenarioClass.getConstructor()
            return (scenarioConstructor.newInstance() as Scenario)::class.java
        }

        private fun getBrowser(commandLine: CommandLine): Class<out Browser> {
            return if (commandLine.hasOption(browserParameter)) {
                val browser = commandLine.getOptionValue(browserParameter)
                val browserClass = Class.forName(browser)
                val browserConstructor = browserClass.getConstructor()
                (browserConstructor.newInstance() as Browser)::class.java
            } else {
                GoogleChrome::class.java
            }
        }
    }
}

private const val deprecatedGetterMessage = "Raise a JPERF Jira story to explain why you need access to this field"
