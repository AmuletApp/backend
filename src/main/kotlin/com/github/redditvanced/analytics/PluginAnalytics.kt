package com.github.redditvanced.analytics

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

object PluginAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		System.getenv("INFLUX_URL"),
		System.getenv("INFLUX_TOKEN").toCharArray(),
		"admin",
		"Plugins",
	)

	@Serializable
	@Measurement(name = "plugin_launch")
	data class PluginLaunch(
		@Column(tag = true)
		val author: String,

		@Column(tag = true)
		val version: String,

		@Column
		val plugin: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)

	@Serializable
	@Measurement(name = "plugin_install")
	data class PluginInstall(
		@Column(tag = true)
		val author: String,

		@Column(tag = true)
		val version: String,

		@Column
		val plugin: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
