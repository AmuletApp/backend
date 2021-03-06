package com.github.redditvanced.publishing

import com.github.redditvanced.Config
import com.github.redditvanced.database
import com.github.redditvanced.database.PublishRequests
import com.github.redditvanced.utils.GithubUtils
import com.github.redditvanced.utils.toBuilder
import net.perfectdreams.discordinteraktions.common.components.ComponentContext
import net.perfectdreams.discordinteraktions.common.components.GuildComponentContext
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.slf4j.LoggerFactory

object ButtonHandler {
	private val logger = LoggerFactory.getLogger(this::class.java)
	private val buttonRegex = "^publishRequest-(\\d+)-(approve|deny|noci)$".toRegex()

	suspend fun onClick(context: ComponentContext) {
		if (context !is GuildComponentContext)
			return

		// Extract the button ID parts
		val (requestId, action) = context.discordInteraction.data.customId.value
			?.let { buttonRegex.find(it)?.destructured }
			?.let { (id, action) -> id.toInt() to action }
			?: return

		// TODO: support deny + noci
		if (action != "approve") {
			context.sendEphemeralMessage {
				content = "Action not currently supported!"
			}
			return
		}

		// Ignore buttons in other servers
		// TODO: add support for multiple servers, one per project
		if (context.guildId.value != Config.DiscordServer.id.value)
			return

		// Check if member has one of the "approve" roles
		val hasPermissions = context.member.roles
			.map { it.value }
			.any { it in Config.DiscordServer.approvingRoles }
		if (!hasPermissions) {
			context.sendMessage {
				content = "You don't have sufficient permissions to approve this commit!"
			}
			return
		}

		// Get plugin request details
		val publishRequest = database
			.sequenceOf(PublishRequests)
			.find { it.id eq requestId }

		// Verify request record still exists
		if (publishRequest == null) {
			context.sendEphemeralMessage {
				content = "Unknown publish request! Feel free to delete the message."
			}
			return
		}

		// Trigger GitHub workflow
		val inputs = GithubUtils.DispatchInputs(
			publishRequest.owner,
			publishRequest.repository,
			publishRequest.plugin,
			publishRequest.targetCommit
		)
		try {
			GithubUtils.triggerPluginBuild(inputs)
		} catch (t: Throwable) {
			val (owner, repo) = inputs
			logger.error("Failed to trigger plugin build workflow! $owner/$repo", t)
		}

		// Update message / disable buttons
		context.updateMessage {
			content = "Building..."
			components = mutableListOf(buildRequestButtons(requestId, true))

			val embed = context.discordInteraction
				.message.value!!
				.embeds.single()
				.toBuilder()
				.apply {
					githubAuthor(inputs.owner)
					color = Config.Colors.ORANGE
				}

			embeds = mutableListOf(embed)
		}
	}
}
