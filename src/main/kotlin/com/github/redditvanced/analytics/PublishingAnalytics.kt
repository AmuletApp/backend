package com.github.redditvanced.analytics

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import java.time.Instant

object PublishingAnalytics {
	private val influx = InfluxDBClientKotlinFactory.create(
		System.getProperty("INFLUX_URL"),
		System.getProperty("INFLUX_TOKEN").toCharArray(),
		"admin",
	)

	suspend fun record(data: Publish) =
		influx.getWriteKotlinApi().writeMeasurement(data, WritePrecision.MS)

	@Measurement(name = "publish_requests")
	data class Publish(
		@Column(tag = true)
		val author: String,

		@Column
		val plugin: String,

		@Column(tag = true)
		val newPlugin: Boolean,

		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
