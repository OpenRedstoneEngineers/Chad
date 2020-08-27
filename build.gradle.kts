import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
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
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "com.github.jkcclemens", name = "khttp", version = "0.1.0")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.13.1")
    implementation(group = "org.javacord", name = "javacord", version = "3.0.3")
    implementation(group = "com.uchuhimo", name = "konf", version = "0.22.1")
    implementation(group = "org.pircbotx", name = "pircbotx", version = "2.1")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.7.9")
    runtime(group = "org.slf4j", name = "slf4j-simple", version = "1.7.29")

    testImplementation(kotlin("test-junit5"))
    testRuntime(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.6.2")
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
