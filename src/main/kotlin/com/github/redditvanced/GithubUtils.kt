package com.github.redditvanced

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object GithubUtils {
	private val http = HttpClient()
	private val githubToken = System.getenv("GITHUB_TOKEN")

	suspend fun getLastSharedCommit(owner: String, repo: String, expectedCommits: List<String>): String? {
		var max = 10 // Max scanned history is (n-1)*100 + 10, so 910 commits
		var commits = emptyList<String>()
		var hasNextPage = true

		while (max-- > 0 && hasNextPage) {
			val data = if (commits.isEmpty())
				getCommits(owner, repo, 10)
			else
				getCommits(owner, repo, 100, after = commits.last())
			commits = data.first
			hasNextPage = data.second

			return expectedCommits.find { it in commits } ?: continue
		}
		return null
	}

	// Return commits + whether there's more commits
	@Suppress("UNCHECKED_CAST")
	suspend fun getCommits(owner: String, repo: String, amount: Int, after: String? = null): Pair<List<String>, Boolean> {
		val gql = """
			{
			  repository(owner: "$owner", name: "$repo") {
			    defaultBranchRef {
			      target {
			        ... on Commit {
			          history(first: $amount ${if (after != null) ", after: \"$after\"" else ""}) {
			            pageInfo {
			              hasNextPage
			            }
			            edges {
			              node {
			                oid
			              }
			            }
			          }
			        }
			      }
			    }
			  }
			}
		""".trimIndent().replace("(\\n|\\s{2,})".toRegex(), "")

		val response = http.get("https://api.github.com/graphql") {
			header("Authentication", "token $githubToken")
			setBody(gql)
		}

		println("requesting github api")

		// TODO: clean up this hot garbage
		val data = response.body<Map<*, *>>() as Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, *>>>>>>

		if (data.containsKey("errors"))
			throw Error(data["errors"].toString())

		val history = data["data"]!!["repository"]!!["defaultBranchRef"]!!["target"]!!["history"]!!
		val hasNextPage = (history["pageInfo"] as Map<*, *>)["hasNextPage"] as Boolean
		val commits = (history["edges"] as List<*>)
			.map { ((it as Map<*, *>)["node"] as Map<*, *>)["oid"] as String }
		return commits to hasNextPage
	}
}
