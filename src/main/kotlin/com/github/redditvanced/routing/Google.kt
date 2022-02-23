package com.github.redditvanced.routing

import com.github.redditvanced.Config
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
	Config.Google.email,
	Config.Google.password,
)

fun Route.configureGoogle() {
	get("/google") {
		call.respond(credentials)
	}
}
