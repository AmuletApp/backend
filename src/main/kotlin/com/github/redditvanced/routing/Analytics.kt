package com.github.redditvanced.routing

import com.github.redditvanced.Project
import com.github.redditvanced.analytics.*
import com.influxdb.client.domain.WritePrecision
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class Science(
	val pluginLaunches: List<PluginAnalytics.PluginLaunch>?,
	val pluginInstall: PluginAnalytics.PluginInstall?,
	val pluginUninstall: PluginAnalytics.PluginUninstall?,
	val install: InstallAnalytics.Install? = null,
	val appLaunch: AppAnalytics.Launch? = null,
)

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.configureAnalytics(project: Project) {
	post<Science>("/science") { data ->
		call.respond("")

		launch {
			PluginAnalytics.influx.getWriteKotlinApi().apply {
				if (data.pluginLaunches?.isNotEmpty() == true)
					writeMeasurements(data.pluginLaunches, WritePrecision.MS, project.influxBucket)
				if (data.pluginInstall != null)
					writeMeasurement(data.pluginInstall, WritePrecision.MS, project.influxBucket)
				if (data.pluginUninstall != null)
					writeMeasurement(data.pluginUninstall, WritePrecision.MS, project.influxBucket)
			}
			if (data.install != null) {
				InstallAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.install, WritePrecision.MS, project.influxBucket)
			}
			if (data.appLaunch != null) {
				AppAnalytics.influx
					.getWriteKotlinApi()
					.writeMeasurement(data.appLaunch, WritePrecision.MS, project.influxBucket)
			}
		}
	}
}
