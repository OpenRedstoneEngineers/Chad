import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.*

plugins {
    application
    java
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.dokka") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "org.openredstone"
version = "1.0"

application {
    mainClassName = "org.openredstone.ChadKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(group = "com.github.jkcclemens", name = "khttp", version = "0.1.0")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.13.1")
    implementation(group = "org.javacord", name = "javacord-core", version = "3.4.0-SNAPSHOT")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.31.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.31.1")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.34.0")
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "org.pircbotx", name = "pircbotx", version = "2.1")
    implementation(group = "io.github.microutils", name = "kotlin-logging-jvm", version = "2.0.6")
    runtimeOnly(group = "org.slf4j", name = "slf4j-simple", version = "1.7.30")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.7.1")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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
