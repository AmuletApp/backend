package com.github.redditvanced.utils

import com.github.redditvanced.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import java.util.zip.ZipFile

object GithubUtils {
	val http = HttpClient {
		defaultRequest {
			header("Authorization", "token ${Config.GitHub.token}")
			header("Accept", "application/vnd.github.v3+json")
			header("Content-Type", "application/json")
		}
		install(ContentNegotiation) {
			json()
		}
	}

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
		val url = "https://api.github.com/repos/${Config.PluginStore.owner}/${Config.PluginStore.repository}" +
			"/actions/workflows/build-plugin.yml/dispatches"
		val res = http.post(url) {
			setBody(DispatchWorkflow("master", data))
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
			query {
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
			val body = buildJsonObject {
				put("query", gql)
			}
			setBody(body)
		}

		val body = response.body<JsonElement>().jsonObject

		if (body.containsKey("errors"))
			throw Error(body["errors"]?.toString())

		val history = body["data"]!!
			.jsonObject["repository"]!!
			.jsonObject["defaultBranchRef"]!!
			.jsonObject["target"]!!
			.jsonObject["history"]!!.jsonObject

		val hasNextPage = history["pageInfo"]!!.jsonObject["hasNextPage"]!!.jsonPrimitive.boolean
		val commits = history["edges"]!!.jsonArray.map {
			it.jsonObject["node"]!!.jsonObject["oid"]!!.jsonPrimitive.content
		}

		return commits to hasNextPage
	}

	private val workflowInputsRegex = "owner:(.+?);repository:(.+?);plugin:(.+?);commit:(.+?);$".toRegex(RegexOption.MULTILINE)
	suspend fun extractWorkflowInputs(run: WorkflowRun): DispatchInputs {
		return withContext(Dispatchers.IO) {
			val bytes = http.get(run.logs_url).body<ByteArray>()
			val file = File("workflow-${UUID.randomUUID()}")
			file.writeBytes(bytes)
			val zip = ZipFile(file)

			val entry = zip.getEntry("build/3_Echo Inputs.txt")
			val log = zip.getInputStream(entry).readBytes().decodeToString()

			zip.close()
			file.delete()

			val results = workflowInputsRegex.find(log)
				?: throw Error("Could not find inputs in workflow log!")

			val (owner, repository, plugin, commit) = results.destructured
			DispatchInputs(owner, repository, plugin, commit)
		}
	}

	@Serializable
	data class WebhookWorkflowModel(
		val action: String,
		val workflow_run: WorkflowRun,
	)

	@Serializable
	data class WorkflowRun(
		val status: String,
		val conclusion: String,
		val html_url: String,
		val logs_url: String,
	)
}

