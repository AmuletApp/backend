package com.github.redditvanced.analytics

import com.github.redditvanced.Config
import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

object PluginAnalytics {
	val influx = InfluxDBClientKotlinFactory.create(
		Config.InfluxDB.url,
		Config.InfluxDB.token.toCharArray(),
		"main",
	)

	@Serializable
	@Measurement(name = "plugin_install")
	data class PluginInstall(
		@Column(tag = true)
		val author: String,

		@Column(tag = true)
		val version: String,

		@Column(tag = true)
		val firstTime: Boolean,

		@Column
		val plugin: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)

	@Serializable
	@Measurement(name = "plugin_uninstall")
	data class PluginUninstall(
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
	@Measurement(name = "plugin_launch")
	data class PluginLaunch(
		@Column(tag = true)
		val author: String,

		@Column(tag = true)
		val version: String,

		@Column(tag = true)
		val enabled: Boolean,

		@Column
		val plugin: String,

		@Transient
		@Column(timestamp = true)
		val time: Instant = Instant.now(),
	)
}
