package com.github.redditvanced.routing

import com.github.redditvanced.*
import com.github.redditvanced.analytics.PublishingAnalytics
import com.github.redditvanced.database.PluginRepos
import com.github.redditvanced.database.PublishRequests
import com.github.redditvanced.publishing.buildRequestButtons
import com.github.redditvanced.publishing.buildRequestEmbed
import com.github.redditvanced.utils.GithubUtils
import com.influxdb.client.domain.WritePrecision
import dev.kord.rest.service.RestClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf

@OptIn(KtorExperimentalLocationsAPI::class)
object Publishing {
	val rest = RestClient(Config.Discord.token)
	private val bannedPlugins = listOf("HelloWorld", "Template")

	@Location("publish/{owner}/{repository}")
	data class PublishRequestRoute(
		val owner: String,
		val repository: String,
		val plugin: String,
	)

	fun Route.configurePublishing(project: Project) {
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
			var existingRequest = database
				.sequenceOf(PublishRequests)
				.find {
					listOf(
						it.owner eq data.owner,
						it.repository eq data.repository,
						it.plugin eq data.plugin
					).reduce { a, b -> a and b }
				}

			// Check if request's message still present
			val existingMessageId = if (existingRequest == null) null else {
				// Verify that message exists
				val message = if (existingRequest.messageId == null) null else {
					try {
						rest.channel.getMessage(Config.DiscordServer.publishingChannel, existingRequest.messageId!!)
					} catch (t: Throwable) {
						null
					}
				}

				// Delete request if message gone
				// This will go on to generate a new request
				if (message == null) {
					database.delete(PublishRequests) { it.id eq existingRequest!!.id }
					existingRequest = null
					null
				} else {
					// Update the target commit to approve in DB and increment updates counter
					database.update(PublishRequests) {
						set(it.targetCommit, commit)
						set(it.updates, it.updates + 1)
						where { it.id eq existingRequest.id }
					}
					existingRequest.messageId
				}
			}

			// Get all existing approved commits (if any, repo might not be registered)
			val knownCommits = database
				.from(PluginRepos)
				.select(PluginRepos.approvedCommits)
				.whereWithConditions {
					it += PluginRepos.owner eq data.owner
					it += PluginRepos.repository eq data.repository
				}
				.rowSet.use {
					if (it.first())
						it[PluginRepos.approvedCommits]
					else null
				}

			// Get the most recent approved commit to compare against
			val lastApprovedCommit = knownCommits?.first()

			// Get the most recent commit that is approved and present on the remote repo
			// This is needed because a force push might have removed it
			val lastSharedCommit = if (knownCommits == null) null else
				GithubUtils.getLastSharedCommit(data.owner, data.repository, knownCommits)

			val messageId = if (existingMessageId == null) {
				// Add request to DB and return the new ID
				val newRequestId = database.insertAndGenerateKey(PublishRequests) {
					set(it.owner, data.owner)
					set(it.repository, data.repository)
					set(it.plugin, data.plugin)
					set(it.targetCommit, commit)
				} as Int

				// Send new publish request message
				val message = rest.channel.createMessage(Config.DiscordServer.publishingChannel) {
					content = "Awaiting approval..."
					embeds += buildRequestEmbed(data, commit, 0, lastApprovedCommit, lastSharedCommit)
					components += buildRequestButtons(newRequestId, false)
				}

				// Update request record to add message id
				database.update(PublishRequests) {
					set(it.messageId, message.id)
					where { it.id eq newRequestId }
				}
				message.id.value
			} else {
				// Edit the existing publish request message with new details
				rest.channel.editMessage(Config.DiscordServer.publishingChannel, existingMessageId) {
					embeds = mutableListOf(buildRequestEmbed(
						data,
						commit,
						existingRequest!!.updates + 1,
						lastApprovedCommit,
						lastSharedCommit
					))
				}
				existingMessageId
			}

			// Record plugin publishing analytics
			launch {
				try {
					val record = PublishingAnalytics.Publish(
						data.owner,
						knownCommits == null && existingRequest == null,
						data.plugin,
					)
					PublishingAnalytics.influx
						.getWriteKotlinApi()	
						.writeMeasurement(record, WritePrecision.MS, project.influxBucket)
				} catch (t: Throwable) {
					application.log.error("Failed to write publishing analytics: ${t.message}")
				}
			}

			@Serializable
			data class Response(
				val message: String,
			)
			call.respond(Response("Success! https://discord.com/${Config.DiscordServer.id}/${Config.DiscordServer.publishingChannel}/$messageId"))
		}
	}
}
