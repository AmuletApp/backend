package com.github.redditvanced.analytics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import java.time.Duration
import java.util.concurrent.Executors

object BackendAnalytics {
	// TODO: don't use micrometer

	private val config = object : InfluxConfig {
		override fun get(key: String) = null
		override fun bucket() = "Backend"
		override fun org() = "admin"
		override fun token(): String =
			System.getenv("INFLUX_TOKEN")
		override fun step(): Duration =
			Duration.ofSeconds(10)
	}
	private val registry = InfluxMeterRegistry(config, Clock.SYSTEM)

	init {
		JvmMemoryMetrics().bindTo(registry)
		ProcessorMetrics().bindTo(registry)
	}

	fun start() =
		registry.start(Executors.defaultThreadFactory())
}
