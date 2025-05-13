package nmcp.internal.task

import gratatouille.GInputFile
import gratatouille.GLogger
import gratatouille.GTask
import java.net.SocketTimeoutException
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import nmcp.CentralPortalOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.ByteString
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow


@GTask(pure = false)
fun publish(
    logger: GLogger,
    username: String?,
    password: String?,
    publicationName: String,
    publishingType: String?,
    baseUrl: String?,
    verificationTimeoutSeconds: Long?,
    inputFile: GInputFile,
) {
    check(!username.isNullOrBlank()) {
        "Ncmp: username is missing"
    }
    check(!password.isNullOrBlank()) {
        "Ncmp: password is missing"
    }

    val token = "$username:$password".let {
        Buffer().writeUtf8(it).readByteString().base64()
    }

    val body = MultipartBody.Builder()
        .addFormDataPart(
            "bundle",
            publicationName,
            inputFile.asRequestBody("application/zip".toMediaType())
        )
        .build()

    val publishingType = publishingType ?: "USER_MANAGED"

    val baseUrl = baseUrl ?: "https://central.sonatype.com/"

    val deploymentId = Request.Builder()
        .post(body)
        .addHeader("Authorization", "UserToken $token")
        .url(baseUrl + "api/v1/publisher/upload?publishingType=$publishingType")
        .build()
        .let {
            client.newCall(it).execute()
        }.use {
            if (!it.isSuccessful) {
                error("Cannot deploy to maven central (status='${it.code}'): ${it.body?.string()}")
            }

            it.body!!.string()
        }

    logger.lifecycle("Nmcp: deployment bundle '$deploymentId' uploaded to '$baseUrl'.")

    val timeout = verificationTimeoutSeconds?.seconds ?: 10.minutes
    var delay = 2.seconds
    if (timeout.isPositive()) {
        logger.lifecycle("Nmcp: verifying deployment status...")
        val mark = markNow()
        while (true) {
            check (mark.elapsedNow() < timeout) {
                "Nmcp: timeout while waiting for the deployment $deploymentId to publish. You might need to check the deployment on the Central Portal UI (see $baseUrl$), or you could increase the wait timeout (the current timeout is $timeout)."
            }
            when (val status = verifyStatus(
                deploymentId = deploymentId,
                baseUrl = baseUrl,
                token = token,
            )) {
                UNKNOWN_QUERY_LATER,
                PENDING,
                VALIDATING,
                PUBLISHING -> {
                    logger.lifecycle("Deployment status is '$status', will try again in ${delay.inWholeSeconds}s (${timeout - mark.elapsedNow()} left)...")
                    // Wait for the next attempt to reduce the load on the Central Portal API
                    Thread.sleep(delay.inWholeMilliseconds)
                    // Increase the delay exponentially, so we don't send too frequent requests if the deployment takes time
                    delay = (delay * 2).coerceAtMost(64.seconds)
                    continue
                }

                VALIDATED -> {
                    logger.lifecycle("Deployment has passed validation, publish it manually from the Central Portal UI.")
                    break
                }

                PUBLISHED -> {
                    logger.lifecycle("Deployment is published.")
                    break
                }

                is FAILED -> {
                    error("Deployment has failed:\n${status.error}")
                }
            }
        }
    }
}

private sealed interface Status

// A deployment has successfully been uploaded to Maven Central
private data object UNKNOWN_QUERY_LATER : Status

// A deployment is uploaded and waiting for processing by the validation service
private data object PENDING : Status

// A deployment is being processed by the validation service
private data object VALIDATING : Status

// A deployment has passed validation and is waiting on a user to manually publish via the Central Portal UI
private data object VALIDATED : Status

// A deployment has been either automatically or manually published and is being uploaded to Maven Central
private data object PUBLISHING : Status

// A deployment has successfully been uploaded to Maven Central
private data object PUBLISHED : Status

// A deployment has encountered an error
private class FAILED(val error: String) : Status

private val client = OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .build()

private fun verifyStatus(
    deploymentId: String,
    baseUrl: String,
    token: String,
): Status {
    Request.Builder()
        .post(ByteString.EMPTY.toRequestBody())
        .addHeader("Authorization", "UserToken $token")
        .url(baseUrl + "api/v1/publisher/status?id=$deploymentId")
        .build()
        .let {
            try {
                client.newCall(it).execute()
            } catch (e: SocketTimeoutException) {
                return UNKNOWN_QUERY_LATER
            }
        }.use {
            if (!it.isSuccessful) {
                error("Cannot verify deployment $deploymentId status (HTTP status='${it.code}'): ${it.body?.string()}")
            }

            val str = it.body!!.string()
            val element = Json.parseToJsonElement(str)
            check(element is JsonObject) {
                "Nmcp: unexpected status response for deployment $deploymentId: $str"
            }

            val state = element["deploymentState"]
            check(state is JsonPrimitive && state.isString) {
                "Nmcp: unexpected deploymentState for deployment $deploymentId: $state"
            }

            return when (state.content) {
                "PENDING" -> PENDING
                "VALIDATING" -> VALIDATING
                "VALIDATED" -> VALIDATED
                "PUBLISHING" -> PUBLISHING
                "PUBLISHED" -> PUBLISHED
                "FAILED" -> {
                    FAILED(element["errors"].toString())
                }
                else -> error("Nmcp: unexpected deploymentState for deployment $deploymentId: $state")
            }
        }
}

internal fun Project.registerPublishTask(
    taskName: String,
    inputFile: Provider<RegularFile>,
    artifactId: Provider<String>,
    spec: CentralPortalOptions
): TaskProvider<PublishTask> {
    val defaultPublicationName = artifactId.map { "${project.group}:${it}:${project.version}.zip" }
    return registerPublishTask(
        taskName = taskName,
        inputFile = inputFile,
        username = spec.username,
        password = spec.password,
        publicationName = spec.publicationName.orElse(defaultPublicationName),
        publishingType = spec.publishingType,
        baseUrl = spec.baseUrl,
        verificationTimeoutSeconds = spec.verificationTimeout.map { it.seconds }
    )
}

