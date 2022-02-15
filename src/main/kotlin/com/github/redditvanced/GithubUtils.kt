package com.github.redditvanced

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GithubUtils {
	private val http = HttpClient()
	private val githubToken = System.getenv("GITHUB_TOKEN")
	private val pluginStoreOwner = System.getenv("GITHUB_OWNER")
	private val pluginStoreRepo = System.getenv("PLUGIN_STORE_REPO")

	@Serializable
	private data class DispatchWorkflow(
		val ref: String, // ref of the plugin-store repository
		val inputs: DispatchInputs,
	)

	@Serializable
	data class DispatchInputs(
		val owner: String,
		val repository: String,
		val plugin: String,
		val ref: String,
	)

	suspend fun triggerPluginBuild(data: DispatchInputs) {
		val res = http.post("https://api.github.com/repos/$pluginStoreOwner/$pluginStoreRepo/actions/workflows/build-plugin.yml/dispatches") {
			header("Authorization", "bearer $githubToken")
			header("Accept", "application/vnd.github.v3+json")
			header("Content-Type", "application/json")
			setBody(Json.encodeToString(DispatchWorkflow("master", data)))
		}
		if (!res.status.isSuccess())
			throw Error("Failed to run plugin build workflow for ${data.owner}/${data.repository}: ${res.status} ${res.bodyAsText()}")
	}

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
		"""

		val response = http.post("https://api.github.com/graphql") {
			header("Authorization", "bearer $githubToken")
			header("Accept", "application/vnd.github.v3+json")
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

	private val diffRegex = "\\+\\+\\+ b\\/(.+?)(?:diff|\$)".toRegex(RegexOption.DOT_MATCHES_ALL)
	suspend fun parseDiff(diffUrl: String): List<String> {
		val diff = http.get(diffUrl).bodyAsText()
		val matches = diffRegex.findAll(diff)
		return matches.map { "+++ " + it.groups[1]!!.value }.toList()
	}
}
