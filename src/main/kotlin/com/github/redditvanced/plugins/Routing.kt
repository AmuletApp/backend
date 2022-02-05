package com.github.redditvanced.plugins

import com.github.redditvanced.analytics.AppAnalytics
import com.github.redditvanced.analytics.InstallAnalytics
import com.github.redditvanced.analytics.PluginAnalytics
import com.github.redditvanced.analytics.RequestAnalytics
import com.github.redditvanced.modals.AccountCredentialsModel
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.publishing.DiscordInteractions.configureDiscordInteractions
import com.github.redditvanced.publishing.PublishPluginRoute.configurePublishPluginRoute
import com.influxdb.client.domain.WritePrecision
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.configureRouting() {
	install(Locations)
	install(ContentNegotiation) {
		json(Json {
			ignoreUnknownKeys = true
		})
	}

	install(StatusPages) {
		exception<Throwable> { call, err ->
			log.error("", err)
			call.respondError(
				"An internal error occurred. Please try again later.",
				HttpStatusCode.InternalServerError
			)
		}
	}

	install(MicrometerMetrics) {
		registry = RequestAnalytics.registry
		meterBinders = emptyList()
	}

	routing {
		configurePublishPluginRoute()
		configureDiscordInteractions()

		static {
			resource("/robots.txt", "robots.txt")
		}

		get("/") {
			call.respondText("Hi! :)")
		}

		get("google") {
			if (call.request.userAgent() != "RedditVanced")
				call.respond(HttpStatusCode.NotFound, "")
			else call.respond(AccountCredentialsModel(
				System.getenv("GOOGLE_EMAIL"),
				System.getenv("GOOGLE_PASSWORD")
			))
		}

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
}
