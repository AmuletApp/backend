import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.6.10"
	id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
	id("com.github.johnrengelman.shadow") version "5.2.0"
	id("application")
}

group = "com.github.redditvanced"
version = "1.0.0"

repositories {
	// Because kord decided to use pom.xml amazing
	maven("https://jitpack.io") {
		metadataSources { mavenPom() }
		content {
			includeGroup("com.github.RedditVanced.kord")
		}
	}

	mavenCentral()
	maven("https://redditvanced.ddns.net/maven/releases")
	maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
	maven("https://oss.sonatype.org/content/repositories/snapshots/")
	maven("https://repo.perfectdreams.net/")
	maven("https://jitpack.io")
}

dependencies {
	val ktorVersion = "2.0.0-beta-1"

	// Ktor Server
	implementation("io.ktor:ktor-server-core:$ktorVersion")
	implementation("io.ktor:ktor-server-netty:$ktorVersion")
	implementation("ch.qos.logback:logback-classic:1.2.10")

	// Ktor plugins
	implementation("io.ktor:ktor-server-locations:$ktorVersion")
	implementation("io.ktor:ktor-server-host-common:$ktorVersion")
	implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
	implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
	implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

	// TODO: make custom request analytics
	// Ktor request analytics
	implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
	implementation("io.micrometer:micrometer-registry-influx:1.8.2")

	// Database
	implementation("org.xerial:sqlite-jdbc:3.36.0.3")
	implementation("org.ktorm:ktorm-core:3.4.1")
	implementation("org.flywaydb:flyway-core:8.5.1")

	// GPlay API
	implementation("com.github.theapache64:google-play-api:0.0.9")
	implementation("com.google.protobuf:protobuf-java:3.19.4")

	// Kord
	implementation("com.github.RedditVanced.kord:kord-rest:feature~ktor-2-SNAPSHOT")
	implementation("net.perfectdreams.discordinteraktions:webserver-ktor-kord:0.0.12-SNAPSHOT")

	// InfluxDB client
	implementation("com.influxdb:influxdb-client-kotlin:4.1.0")

	implementation("io.github.cdimascio", "dotenv-kotlin", "6.2.2")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "17"
		freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
	}
}

application {
	mainClassName = "com.github.redditvanced.MainKt"
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
