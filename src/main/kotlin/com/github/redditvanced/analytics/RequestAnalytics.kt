package com.github.redditvanced.analytics

import com.github.redditvanced.Config
import com.github.redditvanced.Project
import io.micrometer.core.instrument.Clock
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import java.time.Duration

object RequestAnalytics {
	private val config = object : InfluxConfig {
		override fun get(key: String) = null
		override fun bucket() = Project.REDDIT_VANCED.influxBucket
		override fun org() = "main"
		override fun uri() = Config.InfluxDB.url
		override fun token() = Config.InfluxDB.token
		override fun step() = Duration.ofSeconds(10)
	}

	val registry = InfluxMeterRegistry(config, Clock.SYSTEM)
}
