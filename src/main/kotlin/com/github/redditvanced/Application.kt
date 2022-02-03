package com.github.redditvanced

import com.github.redditvanced.migrations.M001
import com.github.redditvanced.plugins.configureMonitoring
import com.github.redditvanced.plugins.configureRouting
import gay.solonovamax.exposed.migrations.runMigrations
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger
import java.time.Duration
import java.util.concurrent.Executors

fun main() {
	val database = Database.connect("jdbc:sqlite:./data.db")
	runMigrations(listOf(M001()))

	Runtime.getRuntime().addShutdownHook(Thread {
		exposedLogger.info("Shutting down...")
		database.connector().close()
	})

	val config = object : InfluxConfig {
		override fun get(key: String) = null
		override fun token() = System.getenv("INFLUX_TOKEN")
		override fun bucket() = "Backend"
		override fun org() = "admin"
		override fun step() = Duration.ofSeconds(10)
	}
	val influx = InfluxMeterRegistry(config, Clock.SYSTEM)
	JvmMemoryMetrics().bindTo(influx)
	ProcessorMetrics().bindTo(influx)

	influx.start(Executors.defaultThreadFactory())

	val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
		configureRouting()
		configureMonitoring()
	}
	server.start(wait = true)
}
