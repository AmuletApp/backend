import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val prometheusVersion: String by project
val exposedVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
}

group = "com.github.redditvanced"
version = "1.0.0"

application {
    mainClass.set("com.github.redditvanced.backend.ApplicationKt")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

dependencies {
    // Ktor & logging
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Ktor plugins
    implementation("io.ktor:ktor-server-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

    // Ktor testing
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("gay.solonovamax:exposed-migrations:4.0.0")
}
