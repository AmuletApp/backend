package com.github.redditvanced.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object RedditVersion : Table("reddit_versions") {
    val sha1: Column<String> = varchar("sha1", 40)

    val versionCode: Column<Int> = integer("version_code")

    /** Example: `2022.1.0` */
    val versionName: Column<String> = varchar("version_name", 15, "NOCASE")

    /** Full names, example: `armeabi-v7a`/`arm64-v8a` */
    val architecture: Column<String> = varchar("architecture", 20, "NOCASE")

    /** YYYY-MM-DD format */
    val publishDate: Column<String> = varchar("publish_date", 10)

    /* Size of the apk in bytes, approximated to 0.1mb */
    val sizeBytes: Column<Int> = integer("size_bytes")

    /** The unique part of the download url (`https://apkpure.com/reddit-apk/com.reddit.frontpage/download/%s`) */
    val download: Column<String> = varchar("download", 50)

    override val primaryKey = PrimaryKey(sha1)
}
