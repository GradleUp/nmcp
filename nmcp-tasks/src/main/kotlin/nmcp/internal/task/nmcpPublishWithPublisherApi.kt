package nmcp.internal.task

import gratatouille.tasks.GInputFile
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import nmcp.transport.Success
import nmcp.transport.executeWithRetries
import nmcp.transport.nmcpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.ByteString
import okio.use

@GTask(pure = false)
internal fun nmcpPublishWithPublisherApi(
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
        "Nmcp: username is missing"
    }
    check(!password.isNullOrBlank()) {
        "Nmcp: password is missing"
    }

    val token = "$username:$password".let {
        Buffer().writeUtf8(it).readByteString().base64()
    }

    val body = MultipartBody.Builder()
        .addFormDataPart(
            "bundle",
            publicationName,
            inputFile.asRequestBody("application/octet-stream".toMediaType()),
        )
        .build()

    @Suppress("NAME_SHADOWING")
    val publishingType = publishingType ?: "AUTOMATIC"

    @Suppress("NAME_SHADOWING")
    val baseUrl = baseUrl ?: "https://central.sonatype.com/"
    val url = baseUrl + "api/v1/publisher/upload?publishingType=$publishingType"

    logger.lifecycle("Uploading deployment to '$url'")
    val request = Request.Builder()
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val result = executeWithRetries(logger, nmcpClient, request)

    if (result !is Success) {
        error("Cannot upload deployment to maven central: ($result)}")
    }
    val deploymentId = result.body.use { it.readUtf8() }

    logger.lifecycle("Nmcp: deployment bundle '$deploymentId' uploaded.")

    val timeout1 = validationTimeoutSeconds?.seconds ?: 10.minutes
    if (timeout1.isPositive()) {
        logger.lifecycle("Nmcp: waiting for validation...")
        waitFor(setOf(VALIDATED, PUBLISHING, PUBLISHED), timeout1, logger, deploymentId, baseUrl, token)

        val timeout2 = publishingTimeoutSeconds?.seconds ?: 0.seconds
        if (publishingType == "AUTOMATIC") {
            if (timeout2.isPositive()) {
                logger.lifecycle("Nmcp: deployment is validated, waiting for publication...")
                waitFor(setOf(PUBLISHED), timeout2, logger, deploymentId, baseUrl, token)
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
    target: Set<Status>,
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
            "Nmcp: timeout while checking deployment '$deploymentId'. You might need to check the deployment status on the Central Portal UI (see $baseUrl), or you could increase the timeout."
        }

        val status = verifyStatus(
            logger = logger,
            deploymentId = deploymentId,
            baseUrl = baseUrl,
            token = token,
        )
        if (status is FAILED) {
            error("Nmcp: deployment has failed:\n${status.error}")
        } else if (status in target) {
            return
        } else {
            logger.lifecycle("Nmcp: deployment status is '$status', will try again in ${pollingInterval.inWholeSeconds}s (${(timeout - mark.elapsedNow()).inWholeSeconds.seconds} left)...")
            // Wait for the next attempt to reduce the load on the Central Portal API
            Thread.sleep(pollingInterval.inWholeMilliseconds)
            continue
        }
    }
}

private sealed interface Status

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
    logger: GLogger,
    deploymentId: String,
    baseUrl: String,
    token: String,
): Status {
    val request = Request.Builder()
        .post(ByteString.EMPTY.toRequestBody())
        .addHeader("Authorization", "Bearer $token")
        .url(baseUrl + "api/v1/publisher/status?id=$deploymentId")
        .build()
    val result = executeWithRetries(logger, nmcpClient, request)
    if (result !is Success) {
        error("Cannot verify deployment $deploymentId status ($result)")
    }

    val str = result.body.use { it.readUtf8() }
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
