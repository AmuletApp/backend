package com.github.redditvanced.routing

import com.github.redditvanced.analytics.*
import com.influxdb.client.domain.WritePrecision
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import kotlinx.coroutines.launch

@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.configureAnalytics() {
	@Location("science")
	data class Science(
		val pluginLaunches: List<PluginAnalytics.PluginLaunch>?,
		val pluginInstall: PluginAnalytics.PluginInstall?,
		val pluginUninstall: PluginAnalytics.PluginUninstall?,
		val install: InstallAnalytics.Install? = null,
		val appLaunch: AppAnalytics.Launch? = null,
	)

	post<Science> { data ->
		call.respond("")

		launch {
			PluginAnalytics.influx.getWriteKotlinApi().apply {
				if (data.pluginLaunches?.isNotEmpty() == true)
					writeMeasurements(data.pluginLaunches, WritePrecision.MS)
				if (data.pluginInstall != null)
					writeMeasurement(data.pluginInstall, WritePrecision.MS)
				if (data.pluginUninstall != null)
					writeMeasurement(data.pluginUninstall, WritePrecision.MS)
			}
			if (data.install != null) {
				InstallAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.install, WritePrecision.MS)
			}
			if (data.appLaunch != null) {
				AppAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.appLaunch, WritePrecision.MS)
			}
		}
	}
}
