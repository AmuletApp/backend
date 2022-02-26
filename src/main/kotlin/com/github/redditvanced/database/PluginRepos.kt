package com.github.redditvanced.database

import org.ktorm.entity.Entity
import org.ktorm.schema.*

interface PluginRepo : Entity<PluginRepo> {
	val id: Int
	var owner: String
	var repository: String
	var approvedCommits: List<String>
}

object PluginRepos : Table<PluginRepo>("plugin_repositories") {
	val id = int("id")
		.bindTo { it.id }
		.primaryKey()

	var owner = varchar("owner")
		.bindTo { it.owner }

	var repository = varchar("repository")
		.bindTo { it.repository }

	var approvedCommits = text("approved_commits")
		.transform({ it.split(',') }, { it.joinToString(",") })
		.bindTo { it.approvedCommits }
}
