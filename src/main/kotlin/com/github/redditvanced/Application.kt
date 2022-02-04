package com.github.redditvanced

import com.github.redditvanced.analytics.BackendAnalytics
import com.github.redditvanced.migrations.M001
import com.github.redditvanced.plugins.configureRouting
import gay.solonovamax.exposed.migrations.runMigrations
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.event.Level

fun main() {
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
	}
	server.start(wait = true)
}
