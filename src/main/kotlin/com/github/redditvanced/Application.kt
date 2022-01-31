package com.github.redditvanced

import com.github.redditvanced.migrations.M001
import com.github.redditvanced.plugins.configureMonitoring
import com.github.redditvanced.plugins.configureRouting
import gay.solonovamax.exposed.migrations.runMigrations
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger

fun main() {
	val database = Database.connect("jdbc:sqlite:./data.db")
	runMigrations(listOf(M001()))

	Runtime.getRuntime().addShutdownHook(Thread {
		exposedLogger.info("Shutting down...")
		database.connector().close()
	})

	val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
		configureRouting()
		configureMonitoring()
	}
	server.start(wait = true)
}
