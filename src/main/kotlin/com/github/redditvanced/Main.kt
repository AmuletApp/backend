package com.github.redditvanced

import com.github.redditvanced.analytics.RequestAnalytics
import com.github.redditvanced.migrations.M001_Publishing
import com.github.redditvanced.routing.configureRouting
import com.github.redditvanced.utils.DotEnv
import gay.solonovamax.exposed.migrations.runMigrations
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.event.Level

fun main() {
	DotEnv.loadDotEnv()

	val database = Database.connect("jdbc:sqlite:./data.db")
	runMigrations(listOf(M001_Publishing()))

	Runtime.getRuntime().addShutdownHook(Thread {
		exposedLogger.info("Shutting down...")
		database.connector().close()
	})

	val port = System.getProperty("PORT").toIntOrNull()?.takeIf { it <= 65535 }
		?: throw IllegalArgumentException("Configured port is invalid!")

	val server = embeddedServer(Netty, port, "127.0.0.1") {
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
