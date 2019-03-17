import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlinVersion = "1.2.70"

plugins {
    kotlin("jvm").version("1.2.70")
    id("com.github.johnrengelman.shadow").version("2.0.4")
    id("com.atlassian.performance.tools.gradle-release").version("0.5.0")
    `java-library`
}

configurations.all {
    resolutionStrategy {
        activateDependencyLocking()
        failOnVersionConflict()
        eachDependency {
            when (requested.module.toString()) {
                "commons-codec:commons-codec" -> useVersion("1.10")
                "com.google.code.gson:gson" -> useVersion("2.8.2")
                "org.slf4j:slf4j-api" -> useVersion("1.8.0-alpha2")
                "com.google.code.findbugs:jsr305" -> useVersion("1.3.9")
                "org.jetbrains:annotations" -> useVersion("13.0")
                "org.apache.commons:commons-compress" -> useVersion("1.9")
                "org.testcontainers:testcontainers" -> useVersion("1.10.5")
                "org.testcontainers:selenium" -> useVersion("1.10.5")
                "javax.annotation:javax.annotation-api" -> useVersion("1.3.2")
                "javax.xml.bind:jaxb-api" -> useVersion("2.3.1")
                "org.rnorth.visible-assertions:visible-assertions" -> useVersion("2.1.2")
                "net.java.dev.jna:jna-platform" -> useVersion("5.2.0")
                "net.java.dev.jna:jna" -> useVersion("5.2.0")
            }
            when (requested.group) {
                "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
            }
        }
    }
}

tasks.getByName("jar", Jar::class).apply {
    manifest.attributes["Main-Class"] = "com.atlassian.performance.tools.virtualusers.api.EntryPointKt"
}

tasks.getByName("shadowJar", ShadowJar::class).apply {
    isZip64 = true
}

dependencies {
    api("com.atlassian.performance.tools:jira-actions:[2.2.0,4.0.0)")
    api("com.github.stephenc.jcip:jcip-annotations:1.0-1")

    implementation("com.atlassian.performance.tools:jira-software-actions:[1.3.0,2.0.0)")
    implementation("com.atlassian.performance.tools:concurrency:[1.0.0, 2.0.0)")
    implementation("com.atlassian.performance.tools:io:[1.0.0, 2.0.0)")
    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.glassfish:javax.json:1.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    webdriver().forEach { implementation(it) }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.apache.commons:commons-csv:1.3")
    implementation("commons-cli:commons-cli:1.4")

    log4jCore().forEach { implementation(it) }
    implementation("io.github.bonigarcia:webdrivermanager:1.7.1")

    testCompile("junit:junit:4.12")
    testCompile("org.assertj:assertj-core:3.11.0")
    testCompile("com.atlassian.performance.tools:docker-infrastructure:0.1.2")
}

fun webdriver(): List<String> = listOf(
    "selenium-support",
    "selenium-chrome-driver"
).map { module ->
    "org.seleniumhq.selenium:$module:3.11.0"
} + log4j("jul")

fun log4jCore(): List<String> = log4j(
    "api",
    "core",
    "slf4j-impl"
)

fun log4j(
    vararg modules: String
): List<String> = modules.map { module ->
    "org.apache.logging.log4j:log4j-$module:2.10.0"
}

tasks.wrapper {
    gradleVersion = "5.2.1"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<Test> {
    testLogging.exceptionFormat = TestExceptionFormat.FULL
    maxHeapSize = "256m"
}

tasks.getByName("test", Test::class).apply {
    exclude("**/*IT.class")
}

val testIntegration = task<Test>("testIntegration") {
    include("**/*IT.class")
}

tasks["check"].dependsOn(testIntegration)
