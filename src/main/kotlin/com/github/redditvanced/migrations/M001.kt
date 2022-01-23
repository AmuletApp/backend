package com.github.redditvanced.migrations

import gay.solonovamax.exposed.migrations.Migration
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction

class M001 : Migration() {
    private object RedditVersion : Table("reddit_versions") {
        val sha1 = varchar("sha1", 40)
        override val primaryKey = PrimaryKey(sha1)

        init {
            integer("version_code")
            varchar("version_name", 15)
            varchar("architecture", 20, "NOCASE")
            varchar("publish_date", 10)
            integer("size_bytes")
            varchar("download", 50)
        }
    }

    override fun Transaction.run() {
        SchemaUtils.create(RedditVersion)
    }
}
