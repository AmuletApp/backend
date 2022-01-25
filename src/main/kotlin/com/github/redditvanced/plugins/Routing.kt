@file:OptIn(KtorExperimentalLocationsAPI::class)

package com.github.redditvanced.plugins

import com.github.redditvanced.database.RedditVersion
import com.github.redditvanced.modals.AccountCredentialsModel
import com.github.redditvanced.modals.RedditVersionModal
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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

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

        get("/") {
            call.respondText("Hello World!")
        }
        get<RedditRoute> {
            val op = when {
                it.arch != null ->
                    Op.build { RedditVersion.versionCode eq it.versionCode and (RedditVersion.architecture eq it.arch) }
                else ->
                    Op.build { RedditVersion.versionCode eq it.versionCode }
            }
            val version = transaction {
                RedditVersion.select(op).limit(1).firstOrNull()
            }

            if (version == null)
                call.respondError("Version does not exist!", HttpStatusCode.NotFound)
            else
                call.respond(RedditVersionModal.fromResultRow(version))
        }

//        get("googleAccount") {
//            if (call.request.userAgent() != "RedditVanced Manager")
//                call.respond(HttpStatusCode.NotFound, "")
//            else call.respond(GAccountHandler.getAccountModal())
//        }

        get("google") {
            if (call.request.userAgent() != "RedditVanced Manager")
                call.respond(HttpStatusCode.NotFound, "")
            else call.respond(AccountCredentialsModel(
                System.getenv("GOOGLE_EMAIL"),
                System.getenv("GOOGLE_PASSWORD")
            ))
        }
    }
}

@Location("/reddit/{versionCode}")
class RedditRoute(val versionCode: Int, val arch: String? = null)
