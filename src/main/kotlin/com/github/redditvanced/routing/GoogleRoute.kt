package com.github.redditvanced.routing

import com.github.redditvanced.modals.AccountCredentialsModel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureGoogle() {
	get("google") {
		if (call.request.userAgent() != "RedditVanced")
			call.respond(HttpStatusCode.NotFound, "")
		else call.respond(AccountCredentialsModel(
			System.getenv("GOOGLE_EMAIL"),
			System.getenv("GOOGLE_PASSWORD")
		))
	}
}
