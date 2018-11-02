package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.SeededRandom
import com.atlassian.performance.tools.jiraactions.api.WebJira
import com.atlassian.performance.tools.jiraactions.api.action.Action
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory
import com.atlassian.performance.tools.jiraactions.api.page.wait
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class SimpleScenario : Scenario {

    private val canLogIn = AtomicBoolean(false)

    override fun getActions(jira: WebJira, seededRandom: SeededRandom, meter: ActionMeter): List<Action> {
        return listOf(HardcodedViewIssueAction(jira, meter))
    }

    override fun getSetupAction(jira: WebJira, meter: ActionMeter): Action {
        return SetupAction(jira, canLogIn)
    }

    override fun getLogInAction(jira: WebJira, meter: ActionMeter, userMemory: UserMemory): Action {
        return LogInAction(jira, userMemory, canLogIn)
    }
}

private class SetupAction(
    private val jira: WebJira,
    private val canLogIn: AtomicBoolean
) : Action {

    private val driver: WebDriver = jira.driver

    override fun run() {
        chooseSetupMode()
        setupDatabase()
        setupWizard()
        setupLicense()
        setupAdministratorAccount()
        setupEmailNotifications()
        setupLanguage()
        setupAvatar()
        createSampleProject()
        canLogIn.set(true)
    }

    private fun createSampleProject() {
        waitAndClick(By.id("sampleData"))
        waitAndClick(By.className("create-project-dialog-create-button"))

        val projectNameInputLocator = By.id("name")
        driver.wait(Duration.ofMinutes(1), ExpectedConditions.visibilityOfElementLocated(projectNameInputLocator))
        driver.findElement(projectNameInputLocator).sendKeys("Sample")

        val addProjectButtonLocator = By.className("add-project-dialog-create-button")
        driver.wait(Duration.ofMinutes(10), ExpectedConditions.elementToBeClickable(addProjectButtonLocator))
        driver.findElement(addProjectButtonLocator).click()

        cleanErrorMessages()
    }

    private fun cleanErrorMessages() {
        driver.wait(Duration.ofMinutes(10), ExpectedConditions.visibilityOfElementLocated(By.className("subnavigator-title")))
        driver.findElements(By.className("icon-close")).forEach { closeDialogButton ->
            closeDialogButton.click()
        }
    }

    private fun setupAvatar() {
        waitAndClick(By.className("avatar-picker-done"))
    }

    private fun setupLanguage() {
        waitAndClick(By.id("next"))
    }

    private fun setupEmailNotifications() {
        waitAndClick(By.id("jira-setupwizard-submit"))
    }

    private fun setupAdministratorAccount() {
        val fullnameLocator = By.cssSelector("input[name='fullname']")
        val emailLocator = By.cssSelector("input[name='email']")
        val usernameLocator = By.cssSelector("input[name='username']")
        val passwordLocator = By.cssSelector("input[name='password']")
        val confirmLocator = By.cssSelector("input[name='confirm']")

        driver.wait(Duration.ofMinutes(1), ExpectedConditions.visibilityOfElementLocated(fullnameLocator))
        driver.findElement(fullnameLocator).sendKeys("Admin Fixer")
        driver.findElement(emailLocator).sendKeys("admin@fixer.com")
        driver.findElement(usernameLocator).sendKeys("admin")
        driver.findElement(passwordLocator).sendKeys("admin")
        driver.findElement(confirmLocator).sendKeys("admin")
        driver.findElement(By.id("jira-setupwizard-submit")).click()
    }

    private fun setupLicense() {
        val timebombLicense = """AAABiQ0ODAoPeNp1kk9TwjAQxe/9FJnxXKYpeoCZHqCtgsqfgaIO4yWELURD0tm0KN/eWOjYdvD68
vbtb3dzM9GKTBgS2iOU9n3a7/pkHiXE96jvbNhho3XnWXBQBuKtyIVWQTxN4sV8MV7GTirMHk5QO
ZJTBsG91eITvPdJBEeQOgN0uNRHwIYtLKWGa1ocNoCzdGUATUA9h2uVdhjPxRGCHAtw5gXyPTMQs
RwCn1Lf9XzXv3NqwVN2gGCZDBYWstLj70zgqSyad0fVWPXgJaClGUfB8KGXuG+rl1v3ab0euUOPv
jofAlmD/XG8GJBY5YAZCtMa9Ze5MagVZAGKX/FVE4eyMDZtqrdgAq+19zJlWEr/Na0TXjkTx4KLj
WzeKbyIjaAJE7aDYpa2tTSO+mvbCrBKo/ryate4Up9KfylnhjumhGEl0SCXzBjB1B9Q/QYhQulrH
/fcue6svl1di8BwFFnZKAGTE3mGIalGksliJxTZVqTmvLF6fXxksjhzpkwaqP5s3fMDBMYhRDAtA
hUAhcR3uL05YCxbclq7h1dNa+Nc+j4CFBrdN005oVlMN9yBlWeM4TlnrOhqX02j3""".trimIndent()
        val licenseKeyLocator = By.id("licenseKey")
        driver.wait(Duration.ofMinutes(2), ExpectedConditions.visibilityOfElementLocated(licenseKeyLocator))
        driver.findElement(licenseKeyLocator).sendKeys(timebombLicense)
        driver.findElement(By.className("aui-button-primary")).sendKeys(timebombLicense)
    }

    private fun setupWizard() {
        waitAndClick(By.id("jira-setupwizard-submit"))
    }

    private fun setupDatabase() {
        waitAndClick(By.id("jira-setup-database-submit"))
    }

    private fun chooseSetupMode() {
        driver.navigate().to(jira.base.toURL())
        waitAndClick(By.cssSelector("div[data-choice-value='classic']"))
        driver.findElement(By.id("jira-setup-mode-submit")).click()
    }

    private fun waitAndClick(by: By, timeout: Duration = Duration.ofMinutes(5)) {
        driver.wait(timeout, ExpectedConditions.visibilityOfElementLocated(by))
        driver.findElement(by).click()
    }
}

private class LogInAction(
    private val jira: WebJira,
    private val userMemory: UserMemory,
    private val canLogIn: AtomicBoolean
) : Action {
    override fun run() {
        if (canLogIn.get()) {
            val user = userMemory.recall()!!
            jira.goToLogin().logIn(user)
            jira.driver.wait(
                Duration.ofSeconds(60),
                ExpectedConditions.presenceOfElementLocated(By.className("page-type-dashboard"))
            )
        }
    }
}

class HardcodedViewIssueAction(
    private val jira: WebJira,
    private val meter: ActionMeter
) : Action {
    val viewIssueAction = ActionType("View Issue") { Unit }
    override fun run() {
        meter.measure(viewIssueAction) {
            jira.goToIssue("SAM-1").waitForSummary()
        }
    }
}