package nmcp.internal.task

import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import kotlin.time.Duration.Companion.seconds
import nmcp.transport.Success
import nmcp.transport.executeWithRetries
import nmcp.transport.nmcpClient
import okhttp3.Request
import okhttp3.RequestBody

@GTask(pure = false)
internal fun nmcpPublishDeployment(
    logger: GLogger,
    username: String?,
    password: String?,
    deploymentId: String?,
    baseUrl: String?,
    publishingTimeoutSeconds: Long?
) {
    check(!deploymentId.isNullOrBlank()) {
        "Nmcp: deploymentId is missing"
    }

    val token = toBearerToken(username, password)

    @Suppress("NAME_SHADOWING")
    val baseUrl = baseUrl ?: "https://central.sonatype.com/"
    val url = baseUrl + "api/v1/publisher/deployment/$deploymentId"

    logger.lifecycle("Publishing previously uploaded deployment bundle '$deploymentId'")
    val request = Request.Builder()
        .post(RequestBody.EMPTY)
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val result = executeWithRetries(logger, nmcpClient, request)

    if (result !is Success) {
        error("Cannot publish deployment '$deploymentId' to maven central: ($result)}")
    }

    logger.lifecycle("Nmcp: deployment bundle '$deploymentId' moved to 'publishing' status.")

    val timeout = publishingTimeoutSeconds?.seconds ?: 0.seconds
    if (timeout.isPositive()) {
        logger.lifecycle("Nmcp: waiting for publication...")
        waitForStatus(setOf(PUBLISHED), timeout, logger, deploymentId, baseUrl, token)
        logger.lifecycle("Nmcp: deployment is published.")
    } else {
        logger.lifecycle("Nmcp: deployment is publishing... Check the central portal UI to verify its status.")
    }
}
