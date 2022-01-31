package com.github.redditvanced.migrations

import gay.solonovamax.exposed.migrations.Migration
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

class M001 : Migration() {
	private object PublishRequest : IntIdTable("publish_requests") {
		init {
			varchar("owner", 39, "NOCASE")
			varchar("repo", 100, "NOCASE")
			varchar("plugin", 40, "NOCASE")
			varchar("target_commit", 40)
			long("message_id").nullable()
			integer("updates").default(0)
		}
	}

	private object PluginRepo : IntIdTable("plugin_repos") {
		init {
			varchar("owner", 39, "NOCASE")
			varchar("repo", 100, "NOCASE")
			text("commits", eagerLoading = true)
		}
	}

	override fun Transaction.run() {
		SchemaUtils.create(PublishRequest, PluginRepo)
	}
}
