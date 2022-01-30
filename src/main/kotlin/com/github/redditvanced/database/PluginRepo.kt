package com.github.redditvanced.database

import org.jetbrains.exposed.dao.id.IntIdTable

object PluginRepo : IntIdTable("plugin_repos") {
	val owner = varchar("owner", 39, "NOCASE")
	val repo = varchar("repo", 100, "NOCASE")

	/**
	 * All approved commits' hashes separated by a comma (,)
	 * Sorted by Newest -> Oldest
	 */
	val approvedCommits = text("commits", eagerLoading = true)
}
