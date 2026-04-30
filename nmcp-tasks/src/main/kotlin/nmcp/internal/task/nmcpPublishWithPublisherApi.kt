package nmcp.internal.task

import gratatouille.tasks.GInputFile
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import nmcp.transport.Success
import nmcp.transport.executeWithRetries
import nmcp.transport.nmcpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
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
    val token = toBearerToken(username, password)

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
        waitForStatus(setOf(VALIDATED, PUBLISHING, PUBLISHED), timeout1, logger, deploymentId, baseUrl, token)

        val timeout2 = publishingTimeoutSeconds?.seconds ?: 0.seconds
        if (publishingType == "AUTOMATIC") {
            if (timeout2.isPositive()) {
                logger.lifecycle("Nmcp: deployment is validated, waiting for publication...")
                waitForStatus(setOf(PUBLISHED), timeout2, logger, deploymentId, baseUrl, token)
                logger.lifecycle("Nmcp: deployment is published.")
            } else {
                logger.lifecycle("Nmcp: deployment is publishing... Check the central portal UI to verify its status.")
            }
        } else {
            check(publishingTimeoutSeconds == null) {
                "Nmcp: 'publishingTimeout' has no effect if 'publishingType' is USER_MANAGED. Either set 'publishingType = AUTOMATIC' or remove 'publishingTimeout'"
            }
            logger.lifecycle("Nmcp: deployment has passed validation, publish it manually from the Central Portal UI or call './gradlew nmcpPublishDeployment -PnmcpDeploymentId=$deploymentId'.")
        }
    } else {
        logger.lifecycle("Nmcp: deployment is validating... Check the central portal UI to verify its status.")
    }
}
