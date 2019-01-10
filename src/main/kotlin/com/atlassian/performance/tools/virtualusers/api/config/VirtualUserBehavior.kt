package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser

class VirtualUserBehavior
@Deprecated(
    message = "Use the VirtualUserBehavior.Builder instead"
) constructor(
    @Deprecated(
        message = "There should be no need to display help from Java API. Read the Javadoc or sources instead."
    )
    internal val help: Boolean,
    internal val scenario: Class<out Scenario>,
    val load: VirtualUserLoad,
    internal val seed: Long,
    internal val diagnosticsLimit: Int,
    internal val browser: Class<out Browser>
) {

    @Deprecated(
        message = "Use the VirtualUserBehavior.Builder instead"
    )
    @Suppress("DEPRECATION")
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
        private var load: VirtualUserLoad = VirtualUserLoad()
        private var seed: Long = 12345
        private var diagnosticsLimit: Int = 16
        private var browser: Class<out Browser> = HeadlessChromeBrowser::class.java

        fun scenario(scenario: Class<out Scenario>) = apply { this.scenario = scenario }
        fun load(load: VirtualUserLoad) = apply { this.load = load }
        fun seed(seed: Long) = apply { this.seed = seed }
        fun diagnosticsLimit(diagnosticsLimit: Int) = apply { this.diagnosticsLimit = diagnosticsLimit }
        fun browser(browser: Class<out Browser>) = apply { this.browser = browser }

        constructor(
            behavior: VirtualUserBehavior
        ) : this(
            scenario = behavior.scenario
        ) {
            load = behavior.load
            scenario = behavior.scenario
            diagnosticsLimit = behavior.diagnosticsLimit
            browser = behavior.browser
        }

        @Suppress("DEPRECATION")
        fun build(): VirtualUserBehavior = VirtualUserBehavior(
            scenario = scenario,
            load = load,
            seed = seed,
            diagnosticsLimit = diagnosticsLimit,
            browser = browser
        )
    }
}
