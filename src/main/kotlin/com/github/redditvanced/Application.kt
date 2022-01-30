package com.github.redditvanced

import com.github.redditvanced.PluginPublishing.configurePluginPublishing
import com.github.redditvanced.database.PluginRepo
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.plugins.configureMonitoring
import com.github.redditvanced.plugins.configureRouting
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
	Database.connect("jdbc:sqlite:./data.db")
	//    runMigrations(listOf(M001()))

	// TODO: create migration for this instead
	transaction {
		SchemaUtils.createMissingTablesAndColumns(PublishRequest, PluginRepo)
	}

	val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
		configureRouting()
		configureMonitoring()
		configurePluginPublishing()
	}
	server.start(wait = true)
}
