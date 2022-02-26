package com.github.redditvanced.database

import dev.kord.common.entity.Snowflake
import org.ktorm.entity.Entity
import org.ktorm.schema.*

interface PublishRequest : Entity<PublishRequest> {
	val id: Int
	val owner: String
	val repository: String
	val plugin: String
	val targetCommit: String
	val messageId: Snowflake?
	val updates: Int
}

object PublishRequests : Table<PublishRequest>("publish_requests") {
	val id = int("id")
		.bindTo { it.id }
		.primaryKey()

	val owner = varchar("owner")
		.bindTo { it.owner }

	val repository = varchar("repository")
		.bindTo { it.repository }

	val plugin = varchar("plugin")
		.bindTo { it.plugin }

	var targetCommit = varchar("target_commit")
		.bindTo { it.targetCommit }

	var messageId = snowflake("message_id")
		.bindTo { it.messageId }

	var updates = int("updates")
		.bindTo { it.updates }
}
