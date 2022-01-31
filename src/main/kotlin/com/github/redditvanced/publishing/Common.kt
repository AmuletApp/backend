package com.github.redditvanced.publishing

import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.component.ActionRowBuilder
import io.ktor.server.locations.*
import kotlinx.serialization.Serializable

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

@Serializable
@Location("publish/{owner}/{repo}")
@OptIn(KtorExperimentalLocationsAPI::class)
data class PublishPlugin(
	val owner: String,
	val repo: String,
	val plugin: String,
	val targetCommit: String,
)
