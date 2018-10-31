package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.browsers.Browser

class VirtualUserBehavior
@Deprecated(
    message = "Use the 5-arg constructor instead"
) constructor(
    @Deprecated(
        message = "There should be no need to display help from Java API. Read the Javadoc or sources instead."
    )
    internal val help: Boolean,
    internal val scenario: Class<out Scenario>,
    internal val load: VirtualUserLoad,
    internal val seed: Long,
    internal val diagnosticsLimit: Int,
    internal val browser: Class<out Browser>
) {

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
}
