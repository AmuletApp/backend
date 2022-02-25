package com.github.redditvanced

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import java.io.File
import kotlin.system.exitProcess

object Config {
	private lateinit var config: Dotenv

	fun init() {
		val env = File(".env.local")
		if (!env.exists()) {
			val classLoader = this::class.java.classLoader
			val defaultEnvUri = classLoader.getResource(".env.default")
				?: error("Failed to load default config from jar")
			env.writeText(defaultEnvUri.readText())

			println("env config missing, generating and exiting. Please fill out the configuration and retry.")
			exitProcess(1)
		}

		config = dotenv {
			filename = ".env.local"
		}
	}

	val port by lazyInt("PORT")

	object Google {
		val email by lazyString("GOOGLE_EMAIL")
		val password by lazyString("GOOGLE_PASSWORD")
	}

	object Discord {
		val token by lazyString("DISCORD_TOKEN")
		val publicKey by lazyString("DISCORD_PUBLIC_KEY")
		val applicationId by lazySnowflake("DISCORD_APPLICATION_ID")
	}

	object DiscordServer {
		val id by lazySnowflake("DISCORD_SERVER_ID")
		val publishingChannel by lazySnowflake("DISCORD_PUBLISHING_CHANNEL_ID")
		val approvingRoles by lazyULongList("DISCORD_PUBLISHING_APPROVING_ROLES")
	}

	object GitHub {
		val token by lazyString("GITHUB_TOKEN")
		val webhookSecret by lazyString("GITHUB_WEBHOOK_SECRET")
	}

	object PluginStore {
		val owner by lazyString("PLUGIN_STORE_ORG")
		val repository by lazyString("PLUGIN_STORE_REPO")
	}

	object InfluxDB {
		val url by lazyString("INFLUX_URL")
		val token by lazyString("INFLUX_TOKEN")
	}

	object Colors {
		val RED = Color(255, 92, 92)
		val ORANGE = Color(247, 149, 84)
		val GREEN = Color(76, 255, 76)
		val YELLOW = Color(243, 213, 104)
		// val DISCORD_GRAY = Color(47, 49, 54)
	}

	private fun lazyString(key: String): Lazy<String> = lazy {
		val value = config[key]
			?: throw IllegalArgumentException("$key not configured!")
		value
	}

	private fun lazyInt(key: String): Lazy<Int> = lazy {
		val value = config[key]
			?: throw IllegalArgumentException("$key not configured!")
		value.toIntOrNull()
			?: throw IllegalArgumentException("$key is not a valid Integer!")
	}

	private fun lazySnowflake(key: String): Lazy<Snowflake> = lazy {
		val value = config[key]
			?: throw IllegalArgumentException("$key not configured!")
		val id = value.toULongOrNull()
			?: throw IllegalArgumentException("$key is not a valid ID!")
		Snowflake(id)
	}

	private fun lazyULongList(key: String): Lazy<List<ULong>> = lazy {
		val values = config[key]?.split(',')
			?: throw IllegalArgumentException("$key not configured!")
		values.map {
			it.toULongOrNull()
				?: throw IllegalArgumentException("$key contains invalid IDs!")
		}
	}
}
