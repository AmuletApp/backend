package com.github.redditvanced.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object PublishRequest : IntIdTable("publish_requests") {
	val owner: Column<String> = varchar("owner", 39, "NOCASE")
	val repo: Column<String> = varchar("repo", 100, "NOCASE")
	val plugin: Column<String> = varchar("plugin", 40, "NOCASE")
	val targetCommit: Column<String> = varchar("target_commit", 40)
	val messageId: Column<Long?> = long("message_id").nullable()
	val updates: Column<Int> = integer("updates").default(0)
}
