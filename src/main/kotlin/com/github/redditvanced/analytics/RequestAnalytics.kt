package com.github.redditvanced.analytics

import io.micrometer.core.instrument.Clock
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import java.time.Duration

object RequestAnalytics {
	// TODO: don't use micrometer

	private val config = object : InfluxConfig {
		override fun get(key: String) = null
		override fun bucket() = "Requests"
		override fun org() = "admin"
		override fun uri() = System.getProperty("INFLUX_URL")
		override fun token() = System.getProperty("INFLUX_TOKEN")
		override fun step() = Duration.ofSeconds(10)
	}

	val registry = InfluxMeterRegistry(config, Clock.SYSTEM)
}
