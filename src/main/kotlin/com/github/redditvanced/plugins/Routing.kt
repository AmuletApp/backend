@file:OptIn(KtorExperimentalLocationsAPI::class)

package com.github.redditvanced.plugins

import com.github.redditvanced.modals.AccountCredentialsModel
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.publishing.DiscordInteractions.configureDiscordInteractions
import com.github.redditvanced.publishing.PublishPluginRoute.configurePublishPluginRoute
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

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

	install(CallLogging) {
		level = Level.INFO
		format { "[${it.request.origin.host}] ${it.request.httpMethod.value} ${it.request.uri} - ${it.response.status()}" }
	}

	routing {
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

		configurePublishPluginRoute()
		configureDiscordInteractions()
	}
}
