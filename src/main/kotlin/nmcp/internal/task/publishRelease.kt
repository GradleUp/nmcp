package nmcp.internal.task

import gratatouille.GInputFile
import gratatouille.GLogger
import gratatouille.GTask
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import nmcp.CentralPortalOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
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
fun publishRelease(
    logger: GLogger,
    username: String?,
    password: String?,
    publicationName: String,
    publishingType: String?,
    baseUrl: String?,
    validationTimeoutSeconds: Long?,
    publishingTimeoutSeconds: Long?,
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
            inputFile.asRequestBody("application/zip".toMediaType()),
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

    val timeout1 = validationTimeoutSeconds?.seconds ?: 10.minutes
    if (timeout1.isPositive()) {
        logger.lifecycle("Nmcp: waiting for validation...")
        waitFor(VALIDATED, timeout1, logger, deploymentId, baseUrl, token)

        val timeout2 = publishingTimeoutSeconds?.seconds ?: 0.seconds
        if (publishingType == "AUTOMATIC") {
            if (timeout2.isPositive()) {
                logger.lifecycle("Nmcp: deployment is validated, waiting for publication...")
                waitFor(PUBLISHED, timeout1, logger, deploymentId, baseUrl, token)
                logger.lifecycle("Nmcp: deployment is published.")
            } else {
                logger.lifecycle("Nmcp: deployment is publishing... Check the central portal UI to verify its status.")
            }
        } else {
            check(publishingTimeoutSeconds == null) {
                "Nmcp: 'publishingTimeout' has no effect if 'publishingType' is USER_MANAGED. Either set 'publishingType = AUTOMATIC' or remove 'publishingTimeout'"
            }
            logger.lifecycle("Nmcp: deployment has passed validation, publish it manually from the Central Portal UI.")
        }
    } else {
        logger.lifecycle("Nmcp: deployment is validating... Check the central portal UI to verify its status.")
    }
}

private fun waitFor(
    target: Status,
    timeout: Duration,
    logger: GLogger,
    deploymentId: String,
    baseUrl: String,
    token: String,
) {
    val pollingInterval = 5.seconds
    val mark = markNow()
    while (true) {
        check(mark.elapsedNow() < timeout) {
            "Nmcp: timeout while checking deployment '$deploymentId'. You might need to check the deployment status on the Central Portal UI (see $baseUrl$), or you could increase the timeout."
        }

        val status = verifyStatus(
            deploymentId = deploymentId,
            baseUrl = baseUrl,
            token = token,
        )
        if (status is FAILED) {
            error("Nmcp: deployment has failed:\n${status.error}")
        } else if (status == target) {
            return
        } else {
            logger.lifecycle("Nmcp: deployment status is '$status', will try again in ${pollingInterval.inWholeSeconds}s (${timeout - mark.elapsedNow()} left)...")
            // Wait for the next attempt to reduce the load on the Central Portal API
            Thread.sleep(pollingInterval.inWholeMilliseconds)
            continue
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

internal fun Project.registerPublishReleaseTask(
    taskName: String,
    inputFile: Provider<RegularFile>,
    artifactId: Provider<String>,
    spec: CentralPortalOptions,
): TaskProvider<PublishReleaseTask> {
    val defaultPublicationName = artifactId.map { "${project.group}:${it}:${project.version}.zip" }
    return registerPublishReleaseTask(
        taskName = taskName,
        inputFile = inputFile,
        username = spec.username,
        password = spec.password,
        publicationName = spec.publicationName.orElse(defaultPublicationName),
        publishingType = spec.publishingType,
        baseUrl = spec.baseUrl,
        validationTimeoutSeconds = spec.validationTimeout.map { it.seconds },
        publishingTimeoutSeconds = spec.publishingTimeout.map { it.seconds },

    )
}

