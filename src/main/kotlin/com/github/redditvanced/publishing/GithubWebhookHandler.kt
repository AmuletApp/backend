package com.github.redditvanced.publishing

import com.github.redditvanced.Config
import com.github.redditvanced.database
import com.github.redditvanced.database.PluginRepos
import com.github.redditvanced.database.PublishRequests
import com.github.redditvanced.routing.Discord
import com.github.redditvanced.routing.Publishing
import com.github.redditvanced.utils.GithubUtils
import com.github.redditvanced.utils.toBuilder
import dev.kord.common.entity.Snowflake
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.slf4j.LoggerFactory

object GithubWebhookHandler {
	private val logger = LoggerFactory.getLogger(this::class.java)

	suspend fun handle(data: GithubUtils.WebhookWorkflowModel) {
		// Extract workflow inputs by downloading the workflow log (refer to: "Echo Inputs" workflow step)
		// This is the best way I found to do this since the GitHub API doesn't provide inputs
		val inputs = try {
			GithubUtils.extractWorkflowInputs(data.workflow_run)
		} catch (t: Throwable) {
			logger.error("Failed to extract workflow inputs!", t)
			return
		}
		val workflowConclusion = data.workflow_run.conclusion

		// TODO: support cancelled workflow runs
		if (workflowConclusion != "failure" && workflowConclusion != "success")
			return

		// Fetch publish request, check exists
		val publishRequest = database
			.sequenceOf(PublishRequests)
			.find {
				listOf(
					it.owner eq inputs.owner,
					it.repository eq inputs.repository,
					it.plugin eq inputs.plugin
				).reduce { a, b -> a and b }
			} ?: return

		// TODO: handle this
		// Check MessageId present on publish request
		val messageId = publishRequest.messageId
			?: return

		// Check Discord message still exists
		val message = try {
			Discord.rest.channel.getMessage(
				Config.DiscordServer.publishingChannel,
				Snowflake(messageId)
			)
		} catch (t: Throwable) {
			// Delete request if message gone
			database.delete(PublishRequests) { it.id eq publishRequest.id }
			return
		}

		// Update Discord message
		try {
			Publishing.rest.channel.editMessage(Config.DiscordServer.publishingChannel, Snowflake(messageId)) {
				val embed = message.embeds.single().toBuilder()
				when (workflowConclusion) {
					"failure" -> {
						content = "**Build failure**\n<${data.workflow_run.html_url}>"
						components = mutableListOf(buildRequestButtons(publishRequest.id, false))
						embed.color = Config.Colors.RED
					}
					"success" -> {
						content = "Build success"
						components = mutableListOf()
						embed.color = Config.Colors.GREEN
					}
				}
				embed.githubAuthor(inputs.owner)
				embeds = mutableListOf(embed)
			}
		} catch (t: Throwable) {
			logger.error("Failed to edit message after workflow run ($workflowConclusion). $data. " +
				"Message: https://discord.com/${Config.DiscordServer.id}/${Config.DiscordServer.publishingChannel}/$messageId", t)
			return
		}

		if (workflowConclusion != "success")
			return

		// Delete completed publish request
		database.delete(PublishRequests) { it.id eq publishRequest.id }

		val (commits) = GithubUtils.getCommits(inputs.owner, inputs.repository, 100)
		val rowsAffected = database.update(PluginRepos) {
			set(it.approvedCommits, commits)
			where { (it.owner eq inputs.owner) and (it.repository eq inputs.repository) }
		}

		if (rowsAffected == 0) database.insert(PluginRepos) {
			set(it.owner, inputs.owner)
			set(it.repository, inputs.repository)
			set(it.approvedCommits, commits)
		}
	}
}
