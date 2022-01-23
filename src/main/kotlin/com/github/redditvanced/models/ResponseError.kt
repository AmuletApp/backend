package com.github.redditvanced.models

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ResponseError(
    val message: String,
)

suspend fun ApplicationCall.respondError(msg: String, code: HttpStatusCode) =
    respond(code, ResponseError(msg))
