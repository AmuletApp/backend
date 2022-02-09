package com.github.redditvanced.routing

import com.github.redditvanced.modals.respondError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Routing.configureGithubWebhook() {
	val webhookKey = System.getenv("GITHUB_WEBHOOK_KEY")
	if (webhookKey == null) {
		application.log.warn("Missing GITHUB_WEBHOOK_KEY, disabling Github webhooks!")
		return
	}

	val key = SecretKeySpec(webhookKey.toByteArray(), "HmacSHA1")
	val mac = Mac.getInstance("HmacSHA1").apply { init(key) }

	fun sign(data: String) =
		HexFormat.of().formatHex(mac.doFinal(data.toByteArray()))

	post("github") {
		val body = call.receiveText()
		val computed = "sha1=${sign(body)}"
		val signature = call.request.header("X-Hub-Signature")
			?: return@post call.respondError("Missing signature header!", HttpStatusCode.BadRequest)

		println("signature: $signature computed: $computed")

		val isVerified = MessageDigest.isEqual(computed.toByteArray(), signature.toByteArray())
		if (!isVerified) {
			call.respondError("Could not verify request!", HttpStatusCode.Unauthorized)
			return@post
		}

		val data = Json.parseToJsonElement(body).jsonObject
		application.log.info(data.toString())
	}
}
