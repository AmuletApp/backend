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
		val pluginInstall: PluginAnalytics.PluginInstall? = null,
		val plugins: List<PluginAnalytics.PluginLaunch> = emptyList(),
		val install: InstallAnalytics.Install? = null,
		val launch: AppAnalytics.Launch? = null,
	)

	post<Science> { data ->
		if (data.plugins.size > 50) return@post
		call.respond("")

		launch {
			PluginAnalytics.influx.getWriteKotlinApi().apply {
				if (data.pluginInstall != null)
					writeMeasurement(data.pluginInstall, WritePrecision.MS)
				if (data.plugins.isNotEmpty())
					writeMeasurements(data.plugins, WritePrecision.MS)
			}
			if (data.install != null) {
				InstallAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.install, WritePrecision.MS)
			}
			if (data.launch != null) {
				AppAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.launch, WritePrecision.MS)
			}
		}
	}
}
