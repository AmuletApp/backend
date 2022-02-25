package com.github.redditvanced.routing

import com.github.redditvanced.Config
import com.github.redditvanced.publishing.ButtonHandler
import dev.kord.common.entity.*
import dev.kord.rest.service.RestClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.perfectdreams.discordinteraktions.common.components.ComponentContext
import net.perfectdreams.discordinteraktions.common.components.GuildComponentContext
import net.perfectdreams.discordinteraktions.common.interactions.InteractionData
import net.perfectdreams.discordinteraktions.common.requests.InteractionRequestState
import net.perfectdreams.discordinteraktions.common.requests.RequestBridge
import net.perfectdreams.discordinteraktions.common.utils.Observable
import net.perfectdreams.discordinteraktions.platforms.kord.entities.KordInteractionMember
import net.perfectdreams.discordinteraktions.platforms.kord.entities.KordUser
import net.perfectdreams.discordinteraktions.platforms.kord.entities.messages.KordPublicMessage
import net.perfectdreams.discordinteraktions.platforms.kord.utils.toDiscordInteraKTionsResolvedObjects
import net.perfectdreams.discordinteraktions.webserver.InteractionRequestHandler
import net.perfectdreams.discordinteraktions.webserver.installDiscordInteractions
import net.perfectdreams.discordinteraktions.webserver.requests.managers.WebServerRequestManager

object Discord {
	val rest = RestClient(Config.Discord.token)

	fun Routing.configureDiscord(path: String) =
		installDiscordInteractions(Config.Discord.publicKey, path, InteractionHandler())

	// Based on DiscordInteraKTions internals because it doesn't support handling non-registered buttons
	private class InteractionHandler : InteractionRequestHandler() {
		override suspend fun onPing(call: ApplicationCall) {
			call.respond(buildJsonObject {
				put("type", InteractionResponseType.Pong.type)
			})
		}

		override suspend fun onComponent(call: ApplicationCall, request: DiscordInteraction) {
			val observableState = Observable(InteractionRequestState.NOT_REPLIED_YET)
			val bridge = RequestBridge(observableState)
			bridge.manager = WebServerRequestManager(
				bridge,
				rest,
				Config.Discord.applicationId,
				request.token,
				call
			)

			val guildId = request.guildId.value
			val kordPublicMessage = KordPublicMessage(request.message.value!!)
			val interactionData = InteractionData(request.data.resolved.value?.toDiscordInteraKTionsResolvedObjects())
			val componentType = request.data.componentType.value
				?: error("Component Type is not present in Discord's request! Bug?")
			val kordUser = KordUser(
				request.member.value?.user?.value
					?: request.user.value
					?: error("User & Member missing on interaction request!")
			)

			// If the guild ID is not null, then it means that the interaction happened in a guild!
			val componentContext = if (guildId != null) {
				val member = request.member.value!! // Should NEVER be null!
				val kordMember = KordInteractionMember(
					member,
					KordUser(member.user.value!!) // Also should NEVER be null!
				)

				GuildComponentContext(
					bridge,
					kordUser,
					request.channelId,
					kordPublicMessage,
					interactionData,
					request,
					guildId,
					kordMember
				)
			} else {
				ComponentContext(
					bridge,
					kordUser,
					request.channelId,
					kordPublicMessage,
					interactionData,
					request
				)
			}

			if (componentType != ComponentType.Button)
				return

			ButtonHandler.onClick(componentContext)

			observableState.awaitChange()
		}
	}
}
