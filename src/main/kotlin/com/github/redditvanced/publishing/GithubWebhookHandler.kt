package com.github.redditvanced.publishing

import com.github.redditvanced.Config
import com.github.redditvanced.database.PluginRepo
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.routing.Discord
import com.github.redditvanced.routing.Publishing
import com.github.redditvanced.utils.GithubUtils
import com.github.redditvanced.utils.toBuilder
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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
		val publishRequest = transaction {
			PublishRequest.select {
				PublishRequest.owner eq inputs.owner and
					(PublishRequest.repo eq inputs.repository) and
					(PublishRequest.plugin eq inputs.plugin)
			}.singleOrNull()
		} ?: return

		val publishRequestId = publishRequest[PublishRequest.id].value

		// Check MessageId present on publish request
		val messageId = publishRequest[PublishRequest.messageId]
			?: return

		// Check Discord message still exists
		val message = try {
			Discord.rest.channel.getMessage(
				Config.DiscordServer.publishingChannel,
				Snowflake(messageId)
			)
		} catch (t: Throwable) {
			// Delete request if message gone
			transaction { PublishRequest.deleteById(publishRequestId) }
			return
		}

		// Update Discord message
		try {
			Publishing.rest.channel.editMessage(Config.DiscordServer.publishingChannel, Snowflake(messageId)) {
				val embed = message.embeds.single().toBuilder()
				when (workflowConclusion) {
					"failure" -> {
						content = "**Build failure**\n<${data.workflow_run.html_url}>"
						components = mutableListOf(buildRequestButtons(publishRequestId, false))
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

		// Update approved commits & delete request
		val repoId = transaction {
			PublishRequest.deleteById(publishRequestId)

			with(PluginRepo) {
				slice(id)
					.select { owner eq inputs.owner and (repo eq inputs.repository) }
					.singleOrNull()
					?.get(id)
			}
		}

		val (commits) = GithubUtils.getCommits(inputs.owner, inputs.repository, 100)
		transaction {
			if (repoId != null) {
				PluginRepo.update({ PluginRepo.id eq repoId }) {
					it[approvedCommits] = commits.joinToString(",")
				}
			} else {
				PluginRepo.insert {
					it[owner] = inputs.owner
					it[repo] = inputs.repository
					it[approvedCommits] = commits.joinToString(",")
				}
			}
		}
	}
}
