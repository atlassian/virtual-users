import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinVersion = "1.2.30"

val jar = tasks["jar"] as Jar
jar.manifest.attributes["Main-Class"] = "com.atlassian.performance.tools.virtualusers.api.EntryPointKt";

plugins {
    kotlin("jvm").version("1.2.30")
    id("com.github.johnrengelman.shadow").version("2.0.4")
    id("com.atlassian.performance.tools.gradle-release").version("0.4.1")
    `java-library`
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        eachDependency {
            when (requested.module.toString()) {
                "commons-codec:commons-codec" -> useVersion("1.10")
                "com.google.code.gson:gson" -> useVersion("2.8.2")
                "org.slf4j:slf4j-api" -> useVersion("1.8.0-alpha2")
            }
        }
    }
}

val shadowJar = tasks["shadowJar"] as ShadowJar
shadowJar.isZip64 = true

dependencies {
    api("com.atlassian.performance.tools:jira-actions:[2.0.0,3.0.0)")
    api("com.github.stephenc.jcip:jcip-annotations:1.0-1")

    implementation("com.atlassian.performance.tools:jira-software-actions:[1.0.0,2.0.0)")
    implementation("com.atlassian.performance.tools:concurrency:[1.0.0, 2.0.0)")
    implementation("com.atlassian.performance.tools:io:[1.0.0, 2.0.0)")
    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.glassfish:javax.json:1.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    webdriver().forEach { implementation(it) }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    implementation("org.apache.commons:commons-csv:1.3")
    implementation("commons-cli:commons-cli:1.4")

    log4jCore().forEach { implementation(it) }
    implementation("io.github.bonigarcia:webdrivermanager:1.7.1")

    testCompile("junit:junit:4.12")
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