package com.github.redditvanced

import com.github.redditvanced.analytics.RequestAnalytics
import com.github.redditvanced.routing.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import org.flywaydb.core.Flyway
import org.ktorm.database.Database
import org.slf4j.event.Level

lateinit var database: Database

fun main() {
	Config.init()

	val dbUrl = "jdbc:sqlite:./data.db"
	Flyway.configure()
		.dataSource(dbUrl, null, null)
		.load()
		.migrate()
	database = Database.connect(dbUrl)

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
