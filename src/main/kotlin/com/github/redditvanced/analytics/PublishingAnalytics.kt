package com.github.redditvanced.analytics

import com.github.redditvanced.Config
import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import java.time.Instant

object PublishingAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		Config.InfluxDB.url,
		Config.InfluxDB.token.toCharArray(),
		"main",
	)

	@Measurement(name = "publish_requests")
	data class Publish(
		@Column(tag = true)
		val author: String,

		@Column(tag = true)
		val newPlugin: Boolean,

		@Column
		val plugin: String,

		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
