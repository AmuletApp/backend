package com.github.redditvanced.routing

import com.github.redditvanced.modals.respondError
import com.github.redditvanced.routing.publishing.DiscordInteractions.configureDiscordInteractions
import com.github.redditvanced.routing.publishing.Publishing.configurePublishing
import com.github.redditvanced.routing.publishing.configureGithubWebhook
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
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

	routing {
		route("/redditvanced") {
			configureAnalytics("RedditVanced")
			configureGoogle()
			configurePublishing()
			configureDiscordInteractions()
			configureGithubWebhook()
		}
	}
}
