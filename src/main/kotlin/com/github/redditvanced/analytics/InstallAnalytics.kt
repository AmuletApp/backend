package com.github.redditvanced.analytics

import com.github.redditvanced.Config
import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

/**
 * Collected when RedditVanced is installed by the installer.
 */
object InstallAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		Config.InfluxDB.url,
		Config.InfluxDB.token.toCharArray(),
		"admin",
	)

	/**
	 * Collects every time RedditVanced has been installed by the installer.
	 */
	@Serializable
	@Measurement(name = "devices")
	data class Install(
		@Column
		val arch: String? = null,

		@Column
		val deviceRom: String? = null,

		@Column
		val apiVersion: Int? = null,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
