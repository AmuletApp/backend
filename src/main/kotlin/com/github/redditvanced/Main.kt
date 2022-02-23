package com.github.redditvanced

import com.github.redditvanced.analytics.RequestAnalytics
import com.github.redditvanced.migrations.M001_Publishing
import com.github.redditvanced.routing.configureRouting
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
	Config.init()

	val database = Database.connect("jdbc:sqlite:./data.db")
	runMigrations(listOf(M001_Publishing()))

	Runtime.getRuntime().addShutdownHook(Thread {
		exposedLogger.info("Shutting down...")
		database.connector().close()
	})

	val server = embeddedServer(Netty, Config.port, "127.0.0.1") {
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
