import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinVersion = "1.2.30"

val jar = tasks["jar"] as Jar
jar.manifest.attributes["Main-Class"] = "com.atlassian.performance.tools.virtualusers.EntryPointKt";

plugins {
    kotlin("jvm").version("1.2.30")
    id("com.github.johnrengelman.shadow").version("2.0.4")
    id("com.atlassian.performance.tools.gradle-release").version("0.0.2")
}

val shadowJar = tasks["shadowJar"] as ShadowJar
shadowJar.isZip64 = true

dependencies {
    compile("com.atlassian.performance.tools:concurrency:0.0.1")
    compile("com.atlassian.performance.tools:jira-actions:0.0.1")
    compile("com.atlassian.performance.tools:io:0.0.1")
    compile("com.atlassian.performance.tools:jira-software-actions:0.1.0")
    compile("com.google.guava:guava:23.6-jre")
    compile("org.glassfish:javax.json:1.1")
    compile("org.apache.commons:commons-math3:3.6.1")
    webdriver().forEach { compile(it) }
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compile("org.apache.commons:commons-csv:1.3")
    compile("commons-cli:commons-cli:1.4")
    testCompile("junit:junit:4.12")

    log4jCore().forEach { compile(it) }
    compile("io.github.bonigarcia:webdrivermanager:1.7.1")
    compile("net.jcip:jcip-annotations:1.0")
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

val wrapper = tasks["wrapper"] as Wrapper
wrapper.gradleVersion = "4.9"
wrapper.distributionType = Wrapper.DistributionType.ALL