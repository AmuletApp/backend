package com.github.redditvanced.modals

import com.github.redditvanced.database.RedditVersion
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class RedditVersionModal(
    val sha1: String,
    val versionCode: Int,
    val versionName: String,
    val architecture: String,
    val publishDate: String,
    val sizeBytes: Int,
    val download: String,
) {
    companion object {
        fun fromResultRow(row: ResultRow) = RedditVersionModal(
            row[RedditVersion.sha1],
            row[RedditVersion.versionCode],
            row[RedditVersion.versionName],
            row[RedditVersion.architecture],
            row[RedditVersion.publishDate],
            row[RedditVersion.sizeBytes],
            "https://apkpure.com/reddit-apk/com.reddit.frontpage/download/${row[RedditVersion.download]}?from=popup%2Fversion"
        )
    }
}
