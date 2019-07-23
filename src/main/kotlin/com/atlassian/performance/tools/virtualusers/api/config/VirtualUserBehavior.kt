package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import com.atlassian.performance.tools.virtualusers.api.users.RestUserGenerator
import com.atlassian.performance.tools.virtualusers.api.users.SuppliedUserGenerator
import com.atlassian.performance.tools.virtualusers.api.users.UserGenerator
import com.atlassian.performance.tools.virtualusers.logs.LogConfiguration
import org.apache.logging.log4j.core.config.AbstractConfiguration
import java.time.Duration

/**
 * @param [maxOverhead] Maximum time to be running, but not applying load.
 */
class VirtualUserBehavior private constructor(
    @Deprecated(
        message = "There should be no need to display help from Java API. Read the Javadoc or sources instead."
    )
    internal val help: Boolean,
    internal val scenario: Class<out Scenario>,
    val load: VirtualUserLoad,
    val maxOverhead: Duration,
    internal val seed: Long,
    internal val diagnosticsLimit: Int,
    internal val browser: Class<out Browser>,
    internal val skipSetup: Boolean,
    internal val userGenerator: Class<out UserGenerator>,
    internal val logging: Class<out AbstractConfiguration>
) {

    @Deprecated(
        message = "Use the VirtualUserBehavior.Builder instead"
    )
    constructor(
        help: Boolean,
        scenario: Class<out Scenario>,
        load: VirtualUserLoad,
        seed: Long,
        diagnosticsLimit: Int,
        browser: Class<out Browser>,
        skipSetup: Boolean
    ) : this(
        help = help,
        scenario = scenario,
        load = load,
        maxOverhead = Duration.ofMinutes(5),
        seed = seed,
        diagnosticsLimit = diagnosticsLimit,
        browser = browser,
        skipSetup = skipSetup,
        userGenerator = SuppliedUserGenerator::class.java,
        logging = LogConfiguration::class.java
    )

    @Deprecated(
        message = "Use the VirtualUserBehavior.Builder instead"
    )
    @Suppress("DEPRECATION")
    constructor(
        help: Boolean,
        scenario: Class<out Scenario>,
        load: VirtualUserLoad,
        seed: Long,
        diagnosticsLimit: Int,
        browser: Class<out Browser>
    ) : this(
        help = help,
        scenario = scenario,
        load = load,
        seed = seed,
        diagnosticsLimit = diagnosticsLimit,
        browser = browser,
        skipSetup = false
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use the VirtualUserBehavior.Builder instead"
    )
    constructor(
        scenario: Class<out Scenario>,
        load: VirtualUserLoad,
        seed: Long,
        diagnosticsLimit: Int,
        browser: Class<out Browser>
    ) : this(
        help = false,
        scenario = scenario,
        load = load,
        seed = seed,
        diagnosticsLimit = diagnosticsLimit,
        browser = browser
    )

    @Deprecated(
        message = "Use the VirtualUserBehavior.Builder.load instead",
        replaceWith = ReplaceWith(
            "Builder(this).load(load).build()",
            "com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior.Builder"
        )
    )
    fun withLoad(
        load: VirtualUserLoad
    ): VirtualUserBehavior = Builder(this).load(load).build()

    class Builder(
        private var scenario: Class<out Scenario>
    ) {
        private var load: VirtualUserLoad = VirtualUserLoad.Builder().build()
        private var maxOverhead: Duration = Duration.ofMinutes(5)
        private var seed: Long = 12345
        private var diagnosticsLimit: Int = 16
        private var browser: Class<out Browser> = HeadlessChromeBrowser::class.java
        private var logging: Class<out AbstractConfiguration> = LogConfiguration::class.java
        private var skipSetup = false
        private var userGenerator: Class<out UserGenerator> = RestUserGenerator::class.java

        fun scenario(scenario: Class<out Scenario>) = apply { this.scenario = scenario }
        fun load(load: VirtualUserLoad) = apply { this.load = load }
        fun maxOverhead(maxOverhead: Duration) = apply { this.maxOverhead = maxOverhead }
        fun seed(seed: Long) = apply { this.seed = seed }
        fun diagnosticsLimit(diagnosticsLimit: Int) = apply { this.diagnosticsLimit = diagnosticsLimit }
        fun browser(browser: Class<out Browser>) = apply { this.browser = browser }
        fun logging(logging: Class<out AbstractConfiguration>) = apply { this.logging = logging }
        fun skipSetup(skipSetup: Boolean) = apply { this.skipSetup = skipSetup }

        @Deprecated("Use VirtualUserBehavior.Builder.userGenerator instead")
        fun createUsers(createUsers: Boolean) = apply {
            userGenerator = if (createUsers) {
                RestUserGenerator::class.java
            } else {
                SuppliedUserGenerator::class.java
            }
        }

        fun userGenerator(userGenerator: Class<out UserGenerator>) = apply { this.userGenerator = userGenerator }

        constructor(
            behavior: VirtualUserBehavior
        ) : this(
            scenario = behavior.scenario
        ) {
            load = behavior.load
            maxOverhead = behavior.maxOverhead
            scenario = behavior.scenario
            diagnosticsLimit = behavior.diagnosticsLimit
            browser = behavior.browser
            logging = behavior.logging
            skipSetup = behavior.skipSetup
            userGenerator = behavior.userGenerator
        }

        @Suppress("DEPRECATION")
        fun build(): VirtualUserBehavior = VirtualUserBehavior(
            help = false,
            scenario = scenario,
            load = load,
            maxOverhead = maxOverhead,
            seed = seed,
            diagnosticsLimit = diagnosticsLimit,
            browser = browser,
            logging = logging,
            skipSetup = skipSetup,
            userGenerator = userGenerator
        )
    }
}
