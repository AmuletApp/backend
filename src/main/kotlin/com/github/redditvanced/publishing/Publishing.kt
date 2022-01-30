package com.github.redditvanced.publishing

import com.github.redditvanced.publishing.DiscordInteractions.configureDiscordInteractions
import com.github.redditvanced.publishing.PublishPluginRoute.configurePublishPluginRoute
import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.component.ActionRowBuilder
import io.ktor.server.application.*
import io.ktor.server.routing.*

object Publishing {
	fun Application.configurePluginPublishing() = routing {
		configurePublishPluginRoute()
		configureDiscordInteractions()

		post("github") {
			// TODO: GitHub webhook
		}
	}

	fun buildRequestButtons(requestId: Int, disabled: Boolean) =
		ActionRowBuilder().apply {
			interactionButton(ButtonStyle.Success, "publishRequest-$requestId-approve") {
				this.disabled = disabled
			}
			interactionButton(ButtonStyle.Secondary, "publishRequest-$requestId-noci") {
				this.disabled = disabled
			}
			interactionButton(ButtonStyle.Danger, "publishRequest-$requestId-deny") {
				this.disabled = disabled
			}
		}
}
