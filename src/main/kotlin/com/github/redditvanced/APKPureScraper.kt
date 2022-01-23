package com.github.redditvanced

import com.github.redditvanced.database.RedditVersion
import it.skrape.core.document
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.li
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object APKPureScraper {
    fun start() {
        timer(
            "APKPureScraper",
            true,
            0,
            1.days.inWholeMilliseconds,
        ) { scrape() }
    }

    private val versionRegex = "(.+) \\(\\d+\\)".toRegex()
    private fun scrape() = skrape(HttpFetcher) {
        request {
            url = "https://apkpure.com/reddit-apk/com.reddit.frontpage/versions"
            userAgent = "RedditVanced backend"
        }

        val versions = mutableListOf<RawVersion>()

        val result = scrape()
        result.document.li {
            findAll { this }.forEach {
                println(it.text)
            }
            val singleAPKInfo = div { findAll("ver-info") }.singleOrNull()
            if (singleAPKInfo != null) {
                val info = div { findFirst("ver-info-m") }

                versions.add(RawVersion(
                    sha1 = info.children[8].ownText,
                    version = singleAPKInfo.children[0].ownText,
                    arch = info.children[5].ownText,
                    publishDate = info.children[0].ownText,
                    sizeBytes = info.children[9].ownText,
                    download = info.children[10].children[0].eachHref[0],
                ))
                println(versions.last())
            } else {
                val versionGroupPage = "https://apkpure.com" + a { findFirst { eachHref.single() } }
                println(versionGroupPage)
                // parse versions for these
            }
        }

        transaction {
            versions.forEach { ver ->
                val (verName, verCode) = versionRegex.find(ver.version)!!.destructured
                val size = ver.sizeBytes.dropLast(1024 * 1024).toFloat().times(1).toInt()
                RedditVersion.insertIgnore {
                    it[sha1] = ver.sha1
                    it[versionCode] = verCode.toInt()
                    it[versionName] = verName
                    it[architecture] = ver.arch
                    it[publishDate] = ver.publishDate
                    it[sizeBytes] = size
                    it[download] = ver.download
                }
            }
        }
    }

    data class RawVersion(
        val sha1: String,
        val version: String,
        val arch: String,
        val publishDate: String,
        val sizeBytes: String,
        val download: String,
    )
}
