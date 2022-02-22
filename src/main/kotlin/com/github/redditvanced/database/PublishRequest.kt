package com.github.redditvanced.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere

object PublishRequest : IntIdTable("publish_requests") {
	val owner = varchar("owner", 39, "NOCASE")
	val repo = varchar("repo", 100, "NOCASE")
	val plugin = varchar("plugin", 40, "NOCASE")
	val targetCommit = varchar("target_commit", 40)
	val messageId = long("message_id").nullable()
	val updates = integer("updates").default(0)

	fun deleteById(publishId: Int) =
		deleteWhere { id eq publishId }
}
