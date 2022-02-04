package com.github.redditvanced.analytics

import io.micrometer.core.instrument.Clock
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import java.time.Duration

object RequestAnalytics {
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

	val registry = InfluxMeterRegistry(config, Clock.SYSTEM)
}
