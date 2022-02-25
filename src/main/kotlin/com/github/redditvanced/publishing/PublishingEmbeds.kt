package com.github.redditvanced.publishing

import com.github.redditvanced.Config
import com.github.redditvanced.routing.Publishing
import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder

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

/**
 * Configures an embed's author properties to display a GitHub user
 */
fun EmbedBuilder.githubAuthor(username: String) = author {
	icon = "https://github.com/$username.png?s=32"
	url = "https://github.com/$username"
	name = username
}

fun buildRequestEmbed(
	data: Publishing.PublishRequestRoute,
	commit: String,
	updates: Int,
	lastApprovedCommit: String?,
	lastSharedCommit: String?,
) = EmbedBuilder().apply {
	val (owner, repo, plugin) = data

	url = "https://github.com/$owner/$repo"
	title = "$owner/$repo -> $plugin"
	color = Config.Colors.YELLOW
	githubAuthor(owner)

	val diffUrl = "https://github.com/$owner/$repo/compare/$lastSharedCommit...$commit"

	description = """
			❯ Info
			• Diff: ${if (lastSharedCommit != null) "[Github]($diffUrl)" else "New repository ✨"}
			• Target commit: `$commit`
			• Request updates: $updates

			❯ History
			• Last approved commit: ${if (lastApprovedCommit != null) "`$lastApprovedCommit`" else "None"}
			• Last shared commit: `${lastSharedCommit ?: "N/A"}`
			${if (lastApprovedCommit != lastSharedCommit) "• Force push detected!" else ""}
		""".trimIndent()
}
