package com.github.redditvanced.utils

import dev.kord.common.Color
import dev.kord.common.entity.DiscordEmbed
import dev.kord.common.entity.optional.value
import dev.kord.rest.builder.message.EmbedBuilder

fun DiscordEmbed.toBuilder(): EmbedBuilder {
	return EmbedBuilder().also {
		it.title = title.value
		it.description = description.value
		it.url = url.value
		it.color = color.value?.let(::Color)
		it.image = image.value?.url?.value
		it.footer = footer.value?.toBuilder()
		it.thumbnail = thumbnail.value?.toBuilder()
		it.author = author.value?.toBuilder()
		it.fields = fields.value?.map(DiscordEmbed.Field::toBuilder)?.toMutableList() ?: mutableListOf()
	}
}

fun DiscordEmbed.Footer.toBuilder(): EmbedBuilder.Footer {
	return EmbedBuilder.Footer().also {
		it.text = text
		it.icon = iconUrl.value
	}
}

fun DiscordEmbed.Author.toBuilder(): EmbedBuilder.Author {
	return EmbedBuilder.Author().also {
		it.name = name.value
		it.url = url.value
		it.icon = url.value
	}
}

fun DiscordEmbed.Field.toBuilder(): EmbedBuilder.Field {
	return EmbedBuilder.Field().also {
		it.name = name
		it.value = value
		it.inline = inline.value
	}
}

fun DiscordEmbed.Thumbnail.toBuilder(): EmbedBuilder.Thumbnail =
	EmbedBuilder.Thumbnail().also { it.url = requireNotNull(url.value) }
