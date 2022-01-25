@file:OptIn(KtorExperimentalLocationsAPI::class)

package com.github.redditvanced.plugins

import com.github.redditvanced.modals.AccountCredentialsModel
import com.github.redditvanced.modals.respondError
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(Locations)
    install(ContentNegotiation) { json() }

    install(StatusPages) {
        exception<Throwable> { call, err ->
            log.error("An error occurred", err)
            call.respondError(
                "An internal error occurred. Please try again later.",
                HttpStatusCode.InternalServerError
            )
        }
    }

    routing {
        static {
            resource("/robots.txt", "robots.txt")
        }

//        get("googleAccount") {
//            if (call.request.userAgent() != "RedditVanced")
//                call.respond(HttpStatusCode.NotFound, "")
//            else call.respond(GAccountHandler.getAccountModal())
//        }

        get("google") {
            if (call.request.userAgent() != "RedditVanced")
                call.respond(HttpStatusCode.NotFound, "")
            else call.respond(AccountCredentialsModel(
                System.getenv("GOOGLE_EMAIL"),
                System.getenv("GOOGLE_PASSWORD")
            ))
        }
    }
}
