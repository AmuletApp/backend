package com.github.redditvanced.routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
private data class GoogleAccount(
	val username: String,
	val password: String,
)

private val credentials = GoogleAccount(
	System.getProperty("GOOGLE_EMAIL"),
	System.getProperty("GOOGLE_PASSWORD")
)

fun Route.configureGoogle() {
	get("/google") {
		call.respond(credentials)
	}
}
