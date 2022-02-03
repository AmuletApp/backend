package com.github.redditvanced.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import java.time.Duration
import java.util.concurrent.Executors

fun Application.configureMonitoring() {
	val config = object : InfluxConfig {
		override fun get(key: String) = null
		override fun token() = System.getenv("INFLUX_TOKEN")
		override fun bucket() = "Http"
		override fun org() = "admin"
		override fun step() = Duration.ofSeconds(10)
	}
	val influx = InfluxMeterRegistry(config, Clock.SYSTEM)

	install(MicrometerMetrics) {
		registry = influx
		meterBinders = emptyList()
	}
}
