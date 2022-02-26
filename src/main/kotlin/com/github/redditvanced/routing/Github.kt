package com.github.redditvanced.routing

import com.github.redditvanced.Config
import com.github.redditvanced.publishing.GithubWebhookHandler
import com.github.redditvanced.utils.GithubUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val json = Json {
	ignoreUnknownKeys = true
}

fun Route.configureGithubWebhook() {
	val key = SecretKeySpec(Config.GitHub.webhookSecret.toByteArray(), "HmacSHA1")
	val mac = Mac.getInstance("HmacSHA1").apply { init(key) }

	fun sign(data: String): String =
		HexFormat.of().formatHex(mac.doFinal(data.toByteArray()))

	post("github") {
		// Verify request came from GitHub
		val signature = call.request.header("X-Hub-Signature")
		val body = call.receiveText()
		val computed = "sha1=${sign(body)}"

		val isVerified = MessageDigest.isEqual(computed.toByteArray(), signature?.toByteArray())
		if (!isVerified) {
			call.respondError("Could not verify request!", HttpStatusCode.Unauthorized)
			return@post
		}
		call.respond("")

		// Parse data
		val data = json.parseToJsonElement(body)
		if (data.jsonObject["action"]?.jsonPrimitive?.contentOrNull != "completed")
			return@post
		val model = json.decodeFromJsonElement<GithubUtils.WebhookWorkflowModel>(data)

		launch {
			GithubWebhookHandler.handle(model)
		}
	}
}
