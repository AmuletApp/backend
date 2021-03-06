package com.github.redditvanced.routing

import com.github.redditvanced.Project
import com.github.redditvanced.routing.Discord.configureDiscord
import com.github.redditvanced.routing.Publishing.configurePublishing
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
			configureAnalytics(Project.REDDIT_VANCED)
			configureGoogle()
			configurePublishing(Project.REDDIT_VANCED)
			configureGithubWebhook()
		}
		configureDiscord("/redditvanced/discord")
	}
}
