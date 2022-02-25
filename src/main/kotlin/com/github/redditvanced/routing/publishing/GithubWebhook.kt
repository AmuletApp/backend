package com.github.redditvanced.routing.publishing

import com.github.redditvanced.Config
import com.github.redditvanced.database.PluginRepo
import com.github.redditvanced.database.PublishRequest
import com.github.redditvanced.modals.respondError
import com.github.redditvanced.utils.GithubUtils
import com.github.redditvanced.utils.GithubUtils.DispatchInputs
import dev.kord.common.entity.Snowflake
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipFile
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val json = Json {
	ignoreUnknownKeys = true
}

fun Route.configureGithubWebhook() {
	val key = SecretKeySpec(Config.GitHub.webhookSecret.toByteArray(), "HmacSHA1")
	val mac = Mac.getInstance("HmacSHA1").apply { init(key) }

	fun sign(data: String): String =
		HexFormat.of().formatHex(mac.doFinal(data.toByteArray()))

	post("github") {
		// Verify request came from GitHub
		val signature = call.request.header("X-Hub-Signature")
		val body = call.receiveText()
		val computed = "sha1=${sign(body)}"

		val isVerified = MessageDigest.isEqual(computed.toByteArray(), signature?.toByteArray())
		if (!isVerified) {
			call.respondError("Could not verify request!", HttpStatusCode.Unauthorized)
			return@post
		}
		call.respond("")

		// Parse data
		val data = json.parseToJsonElement(body)
		if (data.jsonObject["action"]?.jsonPrimitive?.contentOrNull != "completed")
			return@post
		val model = json.decodeFromJsonElement<WebhookWorkflowModel>(data)

		// Extract workflow inputs by downloading the workflow log (refer to: "Echo Inputs" workflow step)
		// This is the best way I found to do this since the GitHub API doesn't provide inputs
		val inputs = try {
			extractWorkflowInputs(model.workflow_run)
		} catch (t: Throwable) {
			application.log.error("Failed to extract workflow inputs!", t)
			return@post
		}
		val workflowConclusion = model.workflow_run.conclusion

		// TODO: support cancelled workflow runs
		if (workflowConclusion != "failure" && workflowConclusion != "success")
			return@post

		// Fetch publish request, check exists
		val publishRequest = transaction {
			PublishRequest.select {
				PublishRequest.owner eq inputs.owner and
					(PublishRequest.repo eq inputs.repository) and
					(PublishRequest.plugin eq inputs.plugin)
			}.singleOrNull()
		} ?: return@post

		val publishRequestId = publishRequest[PublishRequest.id].value

		// Check MessageId present on publish request
		val messageId = publishRequest[PublishRequest.messageId]
			?: return@post // TODO: handle this?

		// Check Discord message still exists
		try {
			Publishing.rest.channel.getMessage(
				Config.DiscordServer.publishingChannel,
				Snowflake(messageId)
			)
		} catch (t: Throwable) {
			// Delete request if message gone
			transaction { PublishRequest.deleteById(publishRequestId) }
			return@post
		}

		// Update Discord message
		try {
			Publishing.rest.channel.editMessage(Config.DiscordServer.publishingChannel, Snowflake(messageId)) {
				when (workflowConclusion) {
					"failure" -> {
						content = ":red_circle: **Build failure**\n<${model.workflow_run.html_url}>"
						components = mutableListOf(buildRequestButtons(publishRequestId, false))
					}
					"success" -> {
						content = ":green_circle: Build success"
						components = mutableListOf()
					}
				}
			}
		} catch (t: Throwable) {
			application.log.error("Failed to edit message after workflow run ($workflowConclusion). $model. " +
				"Message: https://discord.com/${Config.DiscordServer.id}/${Config.DiscordServer.publishingChannel}/$messageId", t)
			return@post
		}

		if (workflowConclusion != "success")
			return@post

		// Update approved commits & delete request
		val repoId = transaction {
			PublishRequest.deleteById(publishRequestId)

			with(PluginRepo) {
				slice(id)
					.select { owner eq inputs.owner and (repo eq inputs.repository) }
					.singleOrNull()
					?.get(id)
			}
		}

		val (commits) = GithubUtils.getCommits(inputs.owner, inputs.repository, 100)
		transaction {
			if (repoId != null) {
				PluginRepo.update({ PluginRepo.id eq repoId }) {
					it[approvedCommits] = commits.joinToString(",")
				}
			} else {
				PluginRepo.insert {
					it[owner] = inputs.owner
					it[repo] = inputs.repository
					it[approvedCommits] = commits.joinToString(",")
				}
			}
		}
	}
}

private suspend fun extractWorkflowInputs(run: WorkflowRun): DispatchInputs = withContext(Dispatchers.IO) {
	val bytes = GithubUtils.http.get(run.logs_url).body<ByteArray>()
	val file = File("workflow-${UUID.randomUUID()}")
	file.writeBytes(bytes)
	val zip = ZipFile(file)

	val entry = zip.getEntry("build/3_Echo Inputs.txt")
	val log = zip.getInputStream(entry).readBytes().decodeToString()

	zip.close()
	file.delete()

	val results = "owner:(.+?);repository:(.+?);plugin:(.+?);commit:(.+?);$".toRegex(RegexOption.MULTILINE).find(log)
		?: throw Error("Could not find inputs in workflow log!")

	val (owner, repository, plugin, commit) = results.destructured
	DispatchInputs(owner, repository, plugin, commit)
}

@Serializable
private data class WebhookWorkflowModel(
	val action: String,
	val workflow_run: WorkflowRun,
)

@Serializable
private data class WorkflowRun(
	val status: String,
	val conclusion: String,
	val html_url: String,
	val logs_url: String,
)
