import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.*

plugins {
    application
    java
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.openredstone"
version = "1.0"

application {
    mainClass.set("org.openredstone.chad.ChadKt")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(group = "org.danilopianini", name = "khttp", version = "1.3.1")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.13.1")
    implementation(group = "org.javacord", name = "javacord-core", version = "3.8.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.31.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.31.1")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.34.0")
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "io.github.microutils", name = "kotlin-logging-jvm", version = "2.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    runtimeOnly(group = "org.slf4j", name = "slf4j-simple", version = "1.7.30")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.7.1")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    test {
        useJUnitPlatform()
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.withType<JavaExec> {
    args("config.yaml")
}

tasks.withType<DokkaTask> {
    dokkaSourceSets {
        named("main") {
            includes.from("packages.md")
        }
    }
}
