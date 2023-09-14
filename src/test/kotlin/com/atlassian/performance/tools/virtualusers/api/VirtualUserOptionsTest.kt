package com.atlassian.performance.tools.virtualusers.api

import com.atlassian.performance.tools.jiraactions.api.memories.User
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome
import com.atlassian.performance.tools.virtualusers.api.config.LoadProcessContainer
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.virtualusers.api.users.RestUserGenerator
import com.atlassian.performance.tools.virtualusers.api.users.SuppliedUserGenerator
import com.atlassian.performance.tools.virtualusers.api.users.UserGenerator
import com.atlassian.performance.tools.virtualusers.engine.HttpLoadProcess
import com.atlassian.performance.tools.virtualusers.engine.LoadProcess
import com.atlassian.performance.tools.virtualusers.engine.LoadThreadFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test
import java.net.URI
import java.time.Duration

class VirtualUserOptionsTest {

    @Suppress("DEPRECATION") // test public API (even if deprecated)
    private val optionsTemplate = VirtualUserOptions(
        target = VirtualUserTarget(
            webApplication = URI("http://localhost/jira/"),
            userName = "fred",
            password = "secret"
        ),
        behavior = VirtualUserBehavior.Builder(JiraSoftwareScenario::class.java)
            .load(
                VirtualUserLoad.Builder()
                    .virtualUsers(7)
                    .hold(Duration.ZERO)
                    .ramp(Duration.ofSeconds(15))
                    .flat(Duration.ofMinutes(5))
                    .maxOverallLoad(TemporalRate(33.6, Duration.ofSeconds(1)))
                    .build()
            )
            .seed(352798235)
            .diagnosticsLimit(8)
            .browser(GoogleChrome::class.java)
            .skipSetup(true)
            .createUsers(true)
            .build()
    )

    @Test
    fun shouldConvertToCli() {
        val args = optionsTemplate.toCliArgs()

        assertThat(args)
            .containsSequence(
                "--jira-address",
                "http://localhost/jira/"
            )
            .containsSequence(
                "--login",
                "fred"
            )
            .containsSequence(
                "--password",
                "secret"
            )
            .containsSequence(
                "--scenario",
                "com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario"
            )
            .containsSequence(
                "--seed",
                "352798235"
            )
            .containsSequence(
                "--diagnostics-limit",
                "8"
            )
            .containsSequence(
                "--virtual-users",
                "7"
            )
            .containsSequence(
                "--hold",
                "PT0S"
            )
            .containsSequence(
                "--ramp",
                "PT15S"
            )
            .containsSequence(
                "--flat",
                "PT5M"
            )
            .containsSequence(
                "--max-overall-load",
                "33.6/PT1S"
            )
            .containsSequence(
                "--browser",
                "com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome"
            )
            .contains("--skip-setup")
            .containsSequence(
                "--user-generator",
                "com.atlassian.performance.tools.virtualusers.api.users.RestUserGenerator"
            )
            .containsSequence(
                "--results",
                "."
            )
    }

    @Test
    fun shouldParseItself() {
        val parser = VirtualUserOptions.Parser()

        val cliArgs = optionsTemplate.toCliArgs()
        val reparsedCliArgs = parser.parse(cliArgs).toCliArgs()

        assertThat(reparsedCliArgs).isEqualTo(cliArgs)
    }

    @Test
    fun shouldParseWithCustomLoadProcess() {
        val options = optionsTemplate.withBehavior(
            VirtualUserBehavior.Builder()
                .loadProcess(MockLoadProcess::class.java)
                .build()
        )

        assertThat(options.toCliArgs())
            .containsSequence(
                "--load-process",
                "com.atlassian.performance.tools.virtualusers.api.VirtualUserOptionsTest.MockLoadProcess"
            )
            .containsSequence(
                "--scenario",
                "com.atlassian.performance.tools.virtualusers.engine.DoNotUseWhenEngineIsProvided"
            )
    }

    @Test
    fun shouldParseWithEmptyConstructor() {
        val options = optionsTemplate.withBehavior(
            VirtualUserBehavior.Builder().build()
        )

        assertThat(options.toCliArgs())
            .containsSequence(
                "--load-process",
                "com.atlassian.performance.tools.virtualusers.engine.HttpLoadProcess"
            )
            .containsSequence(
                "--scenario",
                "com.atlassian.performance.tools.virtualusers.engine.DoNotUseWhenEngineIsProvided"
            )
    }

    @Test
    fun shouldReturnSamePathIfValid() {
        val options = optionsTemplate.withJiraAddress(URI("http://localhost:8080/"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/")
    }

    @Test
    fun shouldAppendPathIfMissing() {
        val options = optionsTemplate.withJiraAddress(URI("http://localhost:8080"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/")
    }

    @Test
    fun shouldThrowOnInvalidUri() {
        val thrown = catchThrowable {
            optionsTemplate.withJiraAddress(URI("http://localhost:8080invalid"))
        }

        assertThat(thrown).hasMessageContaining("http://localhost:8080invalid")
    }

    @Test
    fun shouldFixDanglingContextPath() {
        val options = optionsTemplate.withJiraAddress(URI("http://localhost:8080/context-path"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/context-path/")
    }

    @Test
    fun shouldAllowInsecureConnections() {
        val options = optionsTemplate.withAllowInsecureConnections(true)

        assertThat(options.toCliArgs()).contains("--allow-insecure-connections")
    }

    @Test
    fun shouldNotCreateUsers() {
        @Suppress("DEPRECATION") // test public API (even if deprecated)
        val options = optionsTemplate.withBehavior(
            VirtualUserBehavior.Builder(optionsTemplate.behavior)
                .createUsers(false)
                .build()
        )

        val parsedOptions = VirtualUserOptions.Parser().parse(options.toCliArgs())

        assertThat(parsedOptions.behavior.userGenerator.canonicalName).isEqualTo(SuppliedUserGenerator::class.java.canonicalName)
    }

    @Test
    fun shouldCreateUsers() {
        @Suppress("DEPRECATION") // test public API (even if deprecated)
        val options = optionsTemplate.withBehavior(
            VirtualUserBehavior.Builder(optionsTemplate.behavior)
                .createUsers(true)
                .build()
        )

        val parsedOptions = VirtualUserOptions.Parser().parse(options.toCliArgs())

        assertThat(parsedOptions.behavior.userGenerator.canonicalName).isEqualTo(RestUserGenerator::class.java.canonicalName)
    }

    @Test
    fun shouldUseCustomUserGenerator() {
        val options = optionsTemplate.withBehavior(
            VirtualUserBehavior.Builder(optionsTemplate.behavior)
                .userGenerator(CustomRestUserGenerator::class.java)
                .build()
        )

        val parsedOptions = VirtualUserOptions.Parser().parse(options.toCliArgs())

        assertThat(parsedOptions.behavior.userGenerator.canonicalName).isEqualTo(CustomRestUserGenerator::class.java.canonicalName)
    }

    /**
     * Those args were generated by the first current major: v3.0.0.
     * We need to be backwards compatible within a major version.
     * Specifically it was taken from `shouldConvertToCli` test case.
     */
    @Test
    fun shouldParseFirstMajorVersion() {
        val firstMajorArgs = arrayOf(
            "--help",
            "--jira-address",
            "http://localhost/jira/",
            "--login",
            "fred",
            "--password",
            "secret",
            "--virtual-users",
            "10",
            "--hold",
            "PT0S",
            "--ramp",
            "PT15S",
            "--flat",
            "PT5M",
            "--scenario",
            "com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario",
            "--diagnostics-limit",
            "8",
            "--seed",
            "352798235"
        )

        VirtualUserOptions.Parser().parse(firstMajorArgs)
    }

    private fun VirtualUserOptions.withJiraAddress(
        jiraAddress: URI
    ) = withTarget(
        VirtualUserTarget(
            webApplication = jiraAddress,
            userName = target.userName,
            password = target.password
        )
    )

    @Suppress("DEPRECATION")
    private fun VirtualUserOptions.withAllowInsecureConnections(
        allowInsecureConnections: Boolean
    ) = VirtualUserOptions(
        jiraAddress = jiraAddress,
        scenario = scenario,
        virtualUserLoad = virtualUserLoad,
        adminLogin = adminLogin,
        adminPassword = adminPassword,
        diagnosticsLimit = diagnosticsLimit,
        seed = seed,
        allowInsecureConnections = allowInsecureConnections,
        help = help
    )

    private class CustomRestUserGenerator : UserGenerator {
        override fun generateUser(options: VirtualUserOptions): User {
            return User("test", "test")
        }
    }

    private class MockLoadProcess : LoadProcess {
        override fun setUp(container: LoadProcessContainer): LoadThreadFactory {
            throw Exception("no need to call it in this test")
        }
    }
}
