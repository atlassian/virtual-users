import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlinVersion = "1.2.70"
val seleniumVersion = "3.141.59"
val guavaVersion = "25.0-jre"

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
                "org.testcontainers:testcontainers" -> useVersion("1.14.3")
                "org.testcontainers:selenium" -> useVersion("1.14.3")
                "javax.annotation:javax.annotation-api" -> useVersion("1.3.2")
                "javax.xml.bind:jaxb-api" -> useVersion("2.3.1")
                "org.rnorth.visible-assertions:visible-assertions" -> useVersion("2.1.2")
                "net.java.dev.jna:jna-platform" -> useVersion("5.2.0")
                "net.java.dev.jna:jna" -> useVersion("5.2.0")
                "com.google.guava:guava" -> useVersion(guavaVersion)
            }
            when (requested.group) {
                "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
                "org.seleniumhq.selenium" -> useVersion(seleniumVersion)
            }
        }
    }
}

listOf(
    "testCompile",
    "testCompileClasspath",
    "testRuntime",
    "testRuntimeClasspath"
)
    .map { configurations.getByName(it) }
    .forEach {
        it.resolutionStrategy {
            eachDependency {
                when (requested.module.toString()) {
                    "org.apache.httpcomponents:httpclient" -> useVersion("4.5.5")
                    "org.apache.httpcomponents:httpcore" -> useVersion("4.4.6")
                    "org.codehaus.plexus:plexus-utils" -> useVersion("3.1.0")
                    "org.jsoup:jsoup" -> useVersion("1.10.2")
                    "commons-io:commons-io" -> useVersion("2.6")
                    "org.bouncycastle:bcpkix-jdk15on" -> useVersion("1.60")
                    "org.bouncycastle:bcprov-jdk15on" -> useVersion("1.60")
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

    implementation("com.atlassian.data:random-data:1.4.3")
    implementation("com.atlassian.performance.tools:jira-software-actions:[1.3.0,2.0.0)")
    implementation("com.atlassian.performance.tools:jvm-tasks:[1.0.0,2.0.0)")
    implementation("com.atlassian.performance.tools:io:[1.0.0,2.0.0)")
    implementation("com.atlassian.performance.tools:concurrency:[1.1.0,2.0.0)")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.glassfish:javax.json:1.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    webdriver().forEach { implementation(it) }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.apache.commons:commons-csv:1.3")
    implementation("commons-cli:commons-cli:1.4")

    api(log4j("core"))
    log4jCore().forEach { implementation(it) }
    implementation("io.github.bonigarcia:webdrivermanager:1.7.1")

    testCompile("junit:junit:4.12")
    testCompile("org.assertj:assertj-core:3.11.0")
    testCompile("com.atlassian.performance.tools:docker-infrastructure:0.3.3")
    /*
     * Transitively pulls in their shaded `DockerClientImpl`.
     * Gotta hack it this way until any of the following happens:
     * - they stop shading docker-java-core
     * - they shade docker-java-core properly
     * - we stop using testcontainers in docker-infrastructure
     * - we stop using docker-infrastructure
     */
    testCompile("org.testcontainers:testcontainers:1.14.3")
    testCompile("com.github.docker-java:docker-java-api:3.2.5")
    testCompile("com.atlassian.performance.tools:infrastructure:[4.0.0,4.15.0]")
}

fun webdriver(): List<String> = listOf(
    "selenium-support",
    "selenium-chrome-driver"
).map { module ->
    "org.seleniumhq.selenium:$module:$seleniumVersion"
} + log4j("jul")

fun log4jCore(): List<String> = listOf(
    "api",
    "slf4j-impl"
).map { log4j(it) }

fun log4j(
    module: String
): String = "org.apache.logging.log4j:log4j-$module:2.10.0"


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
    failFast = true
}

tasks["check"].dependsOn(testIntegration)
