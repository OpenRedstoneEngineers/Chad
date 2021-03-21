import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
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
    maven("https://dl.bintray.com/kotlin/exposed")
    maven("https://jitpack.io")
}

dependencies {
    implementation(group = "com.github.jkcclemens", name = "khttp", version = "0.1.0")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.13.1")
    implementation(group = "org.javacord", name = "javacord", version = "3.0.6")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.29.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.29.1")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.34.0")
    implementation(group = "com.uchuhimo", name = "konf", version = "0.22.1")
    implementation(group = "org.pircbotx", name = "pircbotx", version = "2.1")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.8.3")
    runtimeOnly(group = "org.slf4j", name = "slf4j-simple", version = "1.7.30")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.6.2")
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
