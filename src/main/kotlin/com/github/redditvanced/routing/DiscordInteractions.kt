package com.github.redditvanced.routing

import com.github.redditvanced.GithubUtils
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.publishing.PublishPlugin
import com.github.redditvanced.publishing.buildRequestButtons
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.optional
import dev.kord.rest.json.request.InteractionApplicationCommandCallbackData
import dev.kord.rest.json.request.InteractionResponseCreateRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import net.perfectdreams.discordinteraktions.verifier.InteractionRequestVerifier
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object DiscordInteractions {
	private val githubOwner = System.getenv("GITHUB_OWNER")
	private val pluginStoreRepo = System.getenv("PLUGIN_STORE_REPO")
	private val verifier = InteractionRequestVerifier(System.getenv("DISCORD_PUBLIC_KEY"))
	private val btnIdRegex = "^publishRequest-(\\d+)-(approve|deny|noci)$".toRegex()
	private val allowedRoles = System
		.getenv("PLUGIN_PUBLISH_REQUESTS_ALLOWED_VERIFY_ROLES")
		.split(',')
		.map { it.toULongOrNull() ?: throw IllegalArgumentException("Failed to parse verify role ids!") }

	@OptIn(KtorExperimentalLocationsAPI::class)
	fun Routing.configureDiscordInteractions() {
		val json = Json {
			ignoreUnknownKeys = true
		}

		post("discord") {
			val signature = call.request.headers["X-Signature-Ed25519"]!!
			val timestamp = call.request.headers["X-Signature-Timestamp"]!!

			// Verify request is sent by Discord
			val text = call.receiveText()
			val verified = verifier.verifyKey(text, signature, timestamp)
			if (!verified) {
				call.respondText("", ContentType.Application.Json, HttpStatusCode.Unauthorized)
				return@post
			}

			val data = Json.parseToJsonElement(text).jsonObject

			// Respond to the interaction
			if (data["type"]!!.jsonPrimitive.int == InteractionType.Ping.type)
				return@post call.respond(buildJsonObject {
					put("type", InteractionResponseType.Pong.type)
				})

			val interaction = json.decodeFromString<DiscordInteraction>(text)
			if (interaction.type == InteractionType.Component && interaction.member.value != null)
				call.respond(handleComponentInteraction(interaction))
			else {
				call.respondError("", HttpStatusCode.InternalServerError)
			}
		}
	}

	private suspend fun handleComponentInteraction(interaction: DiscordInteraction): InteractionResponseCreateRequest {
		// Check if member has one of the "approve" roles
		val hasPermissions = interaction.member.value!!.roles
			.map { it.value }
			.any { it in allowedRoles }

		// Extract the button ID parts
		val idParts = btnIdRegex.find(interaction.data.customId.value!!)

		return if (!hasPermissions)
			ephemeralResponse("You don't have sufficient permissions to approve this commit!")
		else if (idParts == null)
			ephemeralResponse("Unknown button!")
		else {
			val (idStr, action) = idParts.destructured
			val id = idStr.toInt()

			if (action != "approve")
				return ephemeralResponse("TODO")

			// Get plugin request details
			val publishRequest = transaction {
				PublishRequest
					.select { PublishRequest.id eq id }
					.singleOrNull()
			}

			// Verify request still exists
			publishRequest ?: return ephemeralResponse("Unknown plugin publish request!")

			val data = with(PublishRequest) {
				PublishPlugin(
					publishRequest[owner],
					publishRequest[repo],
					publishRequest[plugin],
					publishRequest[targetCommit]
				)
			}

			// TODO: make owner/repo env variable
			// Trigger GitHub workflow & update message status / disable buttons
			return try {
				GithubUtils.triggerPluginBuild(githubOwner, pluginStoreRepo, data)

				InteractionResponseCreateRequest(
					type = InteractionResponseType.UpdateMessage,
					InteractionApplicationCommandCallbackData(
						content = "Building...".optional(),
						components = listOf(DiscordComponent(
							type = ComponentType.ActionRow,
							components = listOf(buildRequestButtons(id, true).build()).optional()
						)).optional()
					).optional()
				)
			} catch (t: Throwable) {
				t.printStackTrace()
				ephemeralResponse(t.message ?: "An unknown error occurred!")
			}
		}
	}

	private fun ephemeralResponse(msg: String) = InteractionResponseCreateRequest(
		InteractionResponseType.ChannelMessageWithSource,
		InteractionApplicationCommandCallbackData(
			content = msg.optional(),
			flags = MessageFlags(MessageFlag.Ephemeral).optional()
		).optional()
	)
}
