package com.github.redditvanced

import com.github.redditvanced.database.PluginRepo
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.modals.respondError
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.json.request.InteractionApplicationCommandCallbackData
import dev.kord.rest.json.request.InteractionResponseCreateRequest
import dev.kord.rest.service.RestClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.perfectdreams.discordinteraktions.verifier.InteractionRequestVerifier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(KtorExperimentalLocationsAPI::class)
object PluginPublishing {
	private val discord = RestClient(System.getenv("DISCORD_TOKEN"))
	private val verifier = InteractionRequestVerifier(System.getenv("DISCORD_PUBLIC_KEY"))
	private val verificationChannel = Snowflake(System.getenv("DISCORD_PUBLISHING_CHANNEL_ID"))
	private val guildId = System.getenv("DISCORD_GUILD_ID")
	private val allowedRoles = System
		.getenv("PLUGIN_PUBLISH_REQUESTS_ALLOWED_VERIFY_ROLES")
		.split(',')
		.map { it.toULongOrNull() ?: throw IllegalArgumentException("Failed to parse verify role ids!") }

	// The current plugin being built
	// This is changed depending on latest approve / reset by GitHub webhook
	private var currentBuild: PublishPlugin? = null

	fun Application.configurePluginPublishing() = routing {
		post("github") {
			// TODO: GitHub webhook
		}

		post("github/currentBuild") {
			data class Response(
				val data: PublishPlugin?,
			)
			call.respond(Response(currentBuild))
		}

		post("discord") {
			val signature = call.request.headers["X-Signature-Ed25519"]!!
			val timestamp = call.request.headers["X-Signature-Timestamp"]!!

			// Verify request is sent by Discord
			val verified = verifier.verifyKey(call.receiveText(), signature, timestamp)
			if (!verified) {
				call.respondText("", ContentType.Application.Json, HttpStatusCode.Unauthorized)
				return@post
			}

			val interaction = call.receive<DiscordInteraction>()

			// Respond to the interaction
			if (interaction.type == InteractionType.Ping)
				call.respond(InteractionResponseCreateRequest(InteractionResponseType.Pong))
			else if (interaction.type == InteractionType.Component && interaction.member.value != null)
				call.respond(handleComponentInteraction(interaction))
			else {
				call.respondError("", HttpStatusCode.InternalServerError)
			}
		}

		post<PublishPlugin> { data ->
			// Checks if request already exists
			val existingRequest = transaction {
				PublishRequest
					.slice(PublishRequest.id, PublishRequest.messageId)
					.select {
						PublishRequest.owner eq data.owner and
							(PublishRequest.repo eq data.repo) and
							(PublishRequest.plugin eq data.plugin)
					}
					.singleOrNull()
			}

			// Check if request's message still present
			val existingMessageId = if (existingRequest == null) null else {
				val requestId = existingRequest[PublishRequest.id]

				// Update the target commit to approve in DB and increment updates counter
				transaction {
					PublishRequest.update({ PublishRequest.id eq requestId }) {
						it[targetCommit] = data.targetCommit
						it[updates] = updates + 1
					}
				}

				// Verify that message exists
				val messageId = existingRequest[PublishRequest.messageId]
				val message = if (messageId == null) null else {
					try {
						discord.channel.getMessage(verificationChannel, Snowflake(messageId))
					} catch (t: Throwable) {
						null
					}
				}

				// Delete request if message gone
				// This will go on to generate a new request
				if (message == null) transaction {
					PublishRequest.deleteWhere { PublishRequest.id eq requestId }
					null
				} else {
					messageId
				}
			}

			// Get all existing approved commits (if any, repo might not be registered)
			val knownCommits = transaction {
				PluginRepo
					.slice(PluginRepo.approvedCommits)
					.select {
						PluginRepo.owner eq data.owner and
							(PluginRepo.repo eq data.repo)
					}
					.singleOrNull()
					?.get(PluginRepo.approvedCommits)
					?.split(',')
			}

			// Get the most recent approved commit to compare against
			val lastApprovedCommit = knownCommits?.first()

			// Get the most recent commit that is approved and present on the remote repo
			// This is needed because a force push might have removed it
			val lastSharedCommit = if (knownCommits == null) null else
				GithubUtils.getLastSharedCommit(data.owner, data.repo, knownCommits)

			val messageId = if (existingMessageId == null) {
				// Add request to DB and return the new ID
				val newRequestId = transaction {
					PublishRequest.insertAndGetId {
						it[owner] = data.owner
						it[repo] = data.repo
						it[plugin] = data.plugin
						it[targetCommit] = data.targetCommit
					}.value
				}

				// Send new publish request message
				val message = discord.channel.createMessage(verificationChannel) {
					content = "Awaiting approval..."
					embeds += buildRequestEmbed(data, 0, lastApprovedCommit, lastSharedCommit)
					actionRow {
						interactionButton(ButtonStyle.Success, "publishRequest-$newRequestId-approve") {}
						interactionButton(ButtonStyle.Secondary, "publishRequest-$newRequestId-noci") {}
						interactionButton(ButtonStyle.Danger, "publishRequest-$newRequestId-deny") {}
					}
				}
				message.id.value
			} else {
				// Edit the existing publish request message with new details
				discord.channel.editMessage(verificationChannel, Snowflake(existingMessageId)) {
					embeds = mutableListOf(buildRequestEmbed(
						data,
						existingRequest!![PublishRequest.updates],
						lastApprovedCommit,
						lastSharedCommit
					))
				}
				existingMessageId
			}

			@Serializable
			data class Response(
				val message: String,
			)
			call.respond(Response("Success! https://discord.com/$guildId/$verificationChannel/$messageId"))
		}
	}

	private fun buildRequestEmbed(
		data: PublishPlugin,
		updates: Int,
		lastApprovedCommit: String?,
		lastSharedCommit: String?,
	) = EmbedBuilder().apply {
		val (owner, repo, plugin, commit) = data

		url = "https://github.com/$owner/$repo"
		title = "$owner/$repo -> $plugin"
		author {
			icon = "https://github.com/$owner.png?s=32"
			url = "https://github.com/$owner"
			name = owner
		}
		description = """
			❯ Info
			• Compare: ${if (lastSharedCommit != null) "[Github](https://github.com/$owner/$repo/compare/$lastSharedCommit...$commit)" else "New repository ✨"}
			• Target commit: `$commit` + previous (if any)
			• Updates to request: $updates

			❯ History
			• Last approved commit: ${if (lastApprovedCommit != null) "`$lastApprovedCommit`" else "None"}
			• Last shared commit: `${lastSharedCommit ?: "N/A"}`
			${if (lastApprovedCommit != lastSharedCommit) "• Force push detected!" else ""}
		""".trimIndent()
	}

	private val btnIdRegex = "^publishRequest-(\\d+)-(approve|deny|noci)$".toRegex()
	private fun handleComponentInteraction(interaction: DiscordInteraction): InteractionResponseCreateRequest {
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

			// TODO: implement queue
			// Deny approving plugins to build while another build is currently running.
			// This is because workflows rely on the "/github/currentBuild" endpoint to get target plugin info.
			if (action == "approve" && currentBuild != null)
				return ephemeralResponse("There is current another plugin being built!\nPlease wait until that finishes in order to approve a build!")

			// Get plugin request details
			val publishRequest = transaction {
				PublishRequest.select { PublishRequest.id eq id }.singleOrNull()
			}

			// Verify request still exists
			publishRequest ?: return ephemeralResponse("Unknown plugin publish request!")

			// TODO: finish this

			InteractionResponseCreateRequest(
				InteractionResponseType.UpdateMessage,
				InteractionApplicationCommandCallbackData(
					content = "abc123here".optional(),
					components = emptyList<DiscordComponent>().optional()
				).optional()
			)
		}
	}

	private fun ephemeralResponse(msg: String) = InteractionResponseCreateRequest(
		InteractionResponseType.ChannelMessageWithSource,
		InteractionApplicationCommandCallbackData(
			content = msg.optional(),
			flags = MessageFlags(MessageFlag.Ephemeral).optional()
		).optional()
	)

	@Serializable
	@Location("publishPlugin/{githubUsername}/{githubRepo}")
	private data class PublishPlugin(
		val owner: String,
		val repo: String,
		val plugin: String,
		val targetCommit: String,
	)
}
