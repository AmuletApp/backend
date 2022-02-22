package com.github.redditvanced.routing.publishing

import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.utils.GithubUtils
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.component.ActionRowBuilder
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
	private val verifier = InteractionRequestVerifier(System.getProperty("DISCORD_PUBLIC_KEY"))
	private val btnIdRegex = "^publishRequest-(\\d+)-(approve|deny|noci)$".toRegex()
	private val allowedRoles = System
		.getProperty("DISCORD_PUBLISHING_APPROVING_ROLES")
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

			// Trigger GitHub workflow & update message status / disable buttons
			return try {
				GithubUtils.triggerPluginBuild(GithubUtils.DispatchInputs(
					publishRequest[PublishRequest.owner],
					publishRequest[PublishRequest.repo],
					publishRequest[PublishRequest.plugin],
					publishRequest[PublishRequest.targetCommit]
				))

				InteractionResponseCreateRequest(
					type = InteractionResponseType.UpdateMessage,
					InteractionApplicationCommandCallbackData(
						content = ":orange_circle: Building...".optional(),
						components = listOf(buildRequestButtons(id, true).build()).optional()
					).optional()
				)
			} catch (t: Throwable) {
				t.printStackTrace()
				ephemeralResponse("An error occurred!")
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

fun buildRequestButtons(requestId: Int, disabled: Boolean) =
	ActionRowBuilder().apply {
		interactionButton(ButtonStyle.Success, "publishRequest-$requestId-approve") {
			label = "Approve"
			this.disabled = disabled
		}
		interactionButton(ButtonStyle.Secondary, "publishRequest-$requestId-noci") {
			label = "Approve [No-CI]"
			this.disabled = disabled
		}
		interactionButton(ButtonStyle.Danger, "publishRequest-$requestId-deny") {
			label = "Deny"
			this.disabled = disabled
		}
	}
