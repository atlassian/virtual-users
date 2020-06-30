package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.virtualusers.api.browsers.CloseableRemoteWebDriver
import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.diagnostics.WebDriverDiagnostics
import com.atlassian.performance.tools.virtualusers.lib.api.Scenario
import java.util.concurrent.atomic.AtomicInteger

// TODO show that RTE can be disabled via setup
class SimpleWebdriverScenario(
    virtualUserTarget: VirtualUserTarget,
    private val meter: ActionMeter
) : Scenario(
    virtualUserTarget, meter
) {
    private val driver: CloseableRemoteWebDriver = HeadlessChromeBrowser().start()
    private val jira: WebJira = WebJira(
        driver = driver.getDriver(),
        base = virtualUserTarget.webApplication,
        adminPassword = "admin"
    )

    override fun before() { //todo don't use hardcoded credentials
        jira.goToLogin().logIn(User(
            "admin",
            "admin"
        ))
    }

    override fun cleanUp() {
        driver.close()
    }

    override fun getActions(): List<Action> {
        val actions = listOf<Action>(HardcodedViewIssueAction(meter, jira))
        val diagnostics = WebDriverDiagnostics(driver.getDriver())
        return actions.map { action -> DiagnosableAction(action, diagnostics, 10) }
    }


    class HardcodedViewIssueAction(
        private val meter: ActionMeter,
        private val jira: WebJira
    ) : Action {
        private val viewIssueAction = ActionType("View Issue") { Unit }

        override fun run() {
            meter.measure(viewIssueAction) {
                jira.goToIssue("DSEI-1").waitForSummary()
            }
        }
    }

    class DiagnosableAction( // should be available in API?
        private val action: Action,
        private val diagnostics: WebDriverDiagnostics,
        limit: Int = Int.MAX_VALUE
    ) : Action {
        companion object {
            private val limitCounter = AtomicInteger(Int.MAX_VALUE)
        }

        init {
            limitCounter.compareAndSet(Int.MAX_VALUE, limit)
        }

        override fun run() {
            try {
                action.run()
            } catch (e: Exception) {
                if (limitCounter.getAndDecrement() > 0) {
                    diagnostics.diagnose(e)
                }
            }
        }
    }

}
