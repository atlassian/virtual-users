import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    dependencies {
        classpath(GradlePlugins.shadow)
    }
}

plugins {
    kotlin("jvm").version(Versions.kotlin)
    maven
}

maven{
    group = "com.atlassian.test.performance"
    version = "0.0.1-SNAPSHOT"
}

val jar = tasks["jar"] as Jar
jar.manifest.attributes["Main-Class"] = "com.atlassian.performance.tools.virtualusers.EntryPointKt";

apply {
    plugin("com.github.johnrengelman.shadow")
}

val shadowJar = tasks["shadowJar"] as ShadowJar
shadowJar.isZip64 = true

dependencies {
    compile(Libs.concurrency)
    compile(Libs.jiraActions)
    compile(Libs.io)
    compile(Libs.jiraSoftwareActions)
    compile(Libs.guava)
    compile(Libs.json)
    compile(Libs.commonsMath3)
    Libs.webdriver().forEach { compile(it) }
    compile(Libs.kotlinStandard)
    compile(Libs.csv)
    compile(Libs.cli)
    testCompile(Libs.junit)

    Libs.log4jCore().forEach { compile(it) }
    compile(Libs.webdriverManager)
    compile(Libs.jcip)
}