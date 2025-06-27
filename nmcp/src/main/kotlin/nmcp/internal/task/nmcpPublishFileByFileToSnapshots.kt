package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import nmcp.transport.HttpTransport
import nmcp.transport.publishFileByFile
import okhttp3.Credentials

@GTask(pure = false)
internal fun nmcpPublishFileByFileToSnapshots(
    logger: GLogger,
    snapshotsUrl: String,
    username: String?,
    password: String?,
    inputFiles: GInputFiles,
) {
    val authorizationHeader = if (username != null) {
        check(!password.isNullOrBlank()) {
            "Nmcp: password is missing"
        }
        /**
         * Should this be "Basic ${Credentials.basic(username, password)}"?
         * I can't find a good reference on this
         */
        Credentials.basic(username, password)
    } else {
        null
    }

    val transport = HttpTransport(
        baseUrl = snapshotsUrl,
        getAuthorization = null,
        putAuthorization = authorizationHeader,
        logger = logger,
    )

    logger.lifecycle("Nmcp: uploading files to $snapshotsUrl")
    publishFileByFile(transport, inputFiles)
}
