package nmcp.internal.task

import gratatouille.tasks.GLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import nmcp.transport.Success
import nmcp.transport.executeWithRetries
import nmcp.transport.nmcpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.ByteString
import okio.use

internal fun waitForStatus(
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

internal sealed interface Status

// A deployment is uploaded and waiting for processing by the validation service
internal data object PENDING : Status

// A deployment is being processed by the validation service
internal data object VALIDATING : Status

// A deployment has passed validation and is waiting on a user to manually publish via the Central Portal UI
internal data object VALIDATED : Status

// A deployment has been either automatically or manually published and is being uploaded to Maven Central
internal data object PUBLISHING : Status

// A deployment has successfully been uploaded to Maven Central
internal data object PUBLISHED : Status

// A deployment has encountered an error
internal class FAILED(val error: String) : Status

internal fun verifyStatus(
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

internal fun toBearerToken(username: String?, password: String?): String {
    check(!username.isNullOrBlank()) {
        "Nmcp: username is missing"
    }
    check(!password.isNullOrBlank()) {
        "Nmcp: password is missing"
    }

    val token = "$username:$password".let {
        Buffer().writeUtf8(it).readByteString().base64()
    }
    return token
}
