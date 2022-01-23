package com.github.redditvanced.models

import com.github.redditvanced.database.RedditVersion
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class ResponseRedditVersion(
    val sha1: String,
    val versionCode: Int,
    val versionName: String,
    val architecture: String,
    val publishDate: String,
    val sizeBytes: Int,
    val download: String,
) {
    companion object {
        fun fromResultRow(row: ResultRow) = ResponseRedditVersion(
            row[RedditVersion.sha1],
            row[RedditVersion.versionCode],
            row[RedditVersion.versionName],
            row[RedditVersion.architecture],
            row[RedditVersion.publishDate],
            row[RedditVersion.sizeBytes],
            "https://apkpure.com" + row[RedditVersion.download]
        )
    }
}
