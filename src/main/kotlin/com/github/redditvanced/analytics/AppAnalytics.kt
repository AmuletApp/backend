package com.github.redditvanced.analytics

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

object AppAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		System.getProperty("INFLUX_URL"),
		System.getProperty("INFLUX_TOKEN").toCharArray(),
		"admin",
	)

	/**
	 * Collects every time the application has been launched.
	 */
	@Serializable
	@Measurement(name = "launches")
	data class Launch(
		/**
		 * The client stores the last launch date and if was yesterday (UTC) then this is true.
		 */
		@Column
		val firstDaily: Boolean,

		@Column
		val pluginCount: Int,

		@Column
		val coreVersion: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
