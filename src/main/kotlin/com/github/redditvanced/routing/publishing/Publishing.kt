package com.github.redditvanced.routing.publishing

import com.github.redditvanced.analytics.PublishingAnalytics
import com.github.redditvanced.database.PluginRepo
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.database.PublishRequest.targetCommit
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.utils.GithubUtils
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.service.RestClient
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(KtorExperimentalLocationsAPI::class)
object Publishing {
	val discord = RestClient(System.getProperty("DISCORD_TOKEN"))
	val publishChannel = Snowflake(System.getProperty("DISCORD_PUBLISHING_CHANNEL_ID"))
	val serverId: String = System.getProperty("DISCORD_SERVER_ID")
	private val bannedPlugins = listOf("HelloWorld", "Template")

	@Location("publish/{owner}/{repository}")
	data class PublishRequestRoute(
		val owner: String,
		val repository: String,
		val plugin: String,
	)

	fun Routing.configurePublishing() {
		post<PublishRequestRoute> { data ->
			// Check for generic plugin names
			if (data.plugin in bannedPlugins) {
				call.respondError("The ${data.plugin} plugin is banned from being published!", HttpStatusCode.BadRequest)
				return@post
			}

			// Fetch the head commit of the default branch on the repo
			val commit = try {
				val (commits) = GithubUtils.getCommits(data.owner, data.repository, 1)
				commits.single()
			} catch (t: Throwable) {
				application.log.info("Failed to retrieve commits for ${data.owner}/${data.repository}: ${t.message}")
				call.respondError("Internal Server Error", HttpStatusCode.InternalServerError)
				return@post
			}

			// Checks if request already exists
			var existingRequest = transaction {
				PublishRequest
					.slice(PublishRequest.id, PublishRequest.messageId, PublishRequest.updates)
					.select {
						PublishRequest.owner eq data.owner and
							(PublishRequest.repo eq data.repository) and
							(PublishRequest.plugin eq data.plugin)
					}
					.singleOrNull()
			}

			// Check if request's message still present
			val existingMessageId = if (existingRequest == null) null else {
				val requestId = existingRequest[PublishRequest.id]

				// Verify that message exists
				val messageId = existingRequest[PublishRequest.messageId]
				val message = if (messageId == null) null else {
					try {
						discord.channel.getMessage(publishChannel, Snowflake(messageId))
					} catch (t: Throwable) {
						null
					}
				}

				// Delete request if message gone
				// This will go on to generate a new request
				if (message == null) transaction {
					PublishRequest.deleteWhere { PublishRequest.id eq requestId }
					existingRequest = null
					null
				} else {
					// Update the target commit to approve in DB and increment updates counter
					transaction {
						PublishRequest.update({ PublishRequest.id eq requestId }) {
							it[targetCommit] = commit
							it[updates] = updates + 1
						}
					}
					messageId
				}
			}

			// Get all existing approved commits (if any, repo might not be registered)
			val knownCommits = transaction {
				PluginRepo
					.slice(PluginRepo.approvedCommits)
					.select {
						PluginRepo.owner eq data.owner and
							(PluginRepo.repo eq data.repository)
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
				GithubUtils.getLastSharedCommit(data.owner, data.repository, knownCommits)

			val messageId = if (existingMessageId == null) {
				// Add request to DB and return the new ID
				val newRequestId = transaction {
					PublishRequest.insertAndGetId {
						it[owner] = data.owner
						it[repo] = data.repository
						it[plugin] = data.plugin
						it[targetCommit] = commit
					}.value
				}

				// Send new publish request message
				val message = discord.channel.createMessage(publishChannel) {
					content = "Awaiting approval..."
					embeds += buildRequestEmbed(data, commit, 0, lastApprovedCommit, lastSharedCommit)
					components += buildRequestButtons(newRequestId, false)
				}

				// Update request record to add message id
				transaction {
					PublishRequest.update({ PublishRequest.id eq newRequestId }) {
						it[messageId] = message.id.value.toLong()
					}
				}
				message.id.value
			} else {
				// Edit the existing publish request message with new details
				discord.channel.editMessage(publishChannel, Snowflake(existingMessageId)) {
					embeds = mutableListOf(buildRequestEmbed(
						data,
						commit,
						existingRequest!![PublishRequest.updates] + 1,
						lastApprovedCommit,
						lastSharedCommit
					))
				}
				existingMessageId
			}

			// Record plugin publishing analytics
			launch {
				try {
					PublishingAnalytics.record(PublishingAnalytics.Publish(
						data.owner,
						data.plugin,
						knownCommits == null && existingRequest == null
					))
				} catch (t: Throwable) {
					application.log.error("Failed to write publishing analytics: ${t.message}")
				}
			}

			@Serializable
			data class Response(
				val message: String,
			)
			call.respond(Response("Success! https://discord.com/$serverId/$publishChannel/$messageId"))
		}
	}

	private suspend fun buildRequestEmbed(
		data: PublishRequestRoute,
		commit: String,
		updates: Int,
		lastApprovedCommit: String?,
		lastSharedCommit: String?,
	) = EmbedBuilder().apply {
		val (owner, repo, plugin) = data

		url = "https://github.com/$owner/$repo"
		title = "$owner/$repo -> $plugin"
		author {
			icon = "https://github.com/$owner.png?s=32"
			url = "https://github.com/$owner"
			name = owner
		}

		val diffUrl = "https://github.com/$owner/$repo/compare/$lastSharedCommit...$commit"
		val diffs = if (lastSharedCommit == null) null else GithubUtils
			.parseDiff("$diffUrl.diff")
			.map { it.replace("```", "\\```") }
			.joinToString("\n") { "```diff\n$it```" }

		description = """
			❯ Info
			• Compare: ${if (lastSharedCommit != null) "[Github]($diffUrl)" else "New repository ✨"}
			• Target commit: `$commit` + previous (if any)
			• Request updates: $updates

			❯ History
			• Last approved commit: ${if (lastApprovedCommit != null) "`$lastApprovedCommit`" else "None"}
			• Last shared commit: `${lastSharedCommit ?: "N/A"}`
			${if (lastApprovedCommit != lastSharedCommit) "• Force push detected!" else ""}

			${if (diffs != null && diffs.length <= 4000) "❯ Changes\n$diffs" else ""}
		""".trimIndent()
	}
}
