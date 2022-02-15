package com.github.redditvanced.analytics

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

object AppAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		System.getenv("INFLUX_URL"),
		System.getenv("INFLUX_TOKEN").toCharArray(),
		"admin",
		"Devices",
	)

	/**
	 * Collects every time the application has been launched.
	 */
	@Serializable
	@Measurement(name = "launches")
	data class Launch(
		/**
		 * This is included so that we can tell amount of total users a day.
		 * (App will be launched multiple times a day)
		 */
		@Column
		val sha1Username: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
