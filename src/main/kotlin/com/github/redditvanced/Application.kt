package com.github.redditvanced

import com.github.redditvanced.analytics.BackendAnalytics
import com.github.redditvanced.analytics.RequestAnalytics
import com.github.redditvanced.migrations.M001
import com.github.redditvanced.routing.configureRouting
import gay.solonovamax.exposed.migrations.runMigrations
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.event.Level
import java.io.File
import kotlin.system.exitProcess

fun main() {
	// Load env variables
	val env = File(".env.local")
	if (!env.exists()) {
		val classLoader = M001::class.java.classLoader
		val defaultEnvUri = classLoader.getResource(".env.default")
			?: error("Failed to load default config from jar")
		env.writeText(defaultEnvUri.readText())

		println("env config missing, generating and exiting. Please fill out the configuration and retry.")
		exitProcess(1)
	}

	dotenv {
		filename = ".env.local"
		systemProperties = true
	}

	val database = Database.connect("jdbc:sqlite:./data.db")
	runMigrations(listOf(M001()))

	Runtime.getRuntime().addShutdownHook(Thread {
		exposedLogger.info("Shutting down...")
		database.connector().close()
	})

	BackendAnalytics.start()

	val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
		configureRouting()

		install(CallLogging) {
			level = Level.INFO
			format { "[${it.request.origin.host}] ${it.request.httpMethod.value} ${it.request.uri} - ${it.response.status()}" }
		}

		install(MicrometerMetrics) {
			registry = RequestAnalytics.registry
			meterBinders = emptyList()
		}
	}
	server.start(wait = true)
}
