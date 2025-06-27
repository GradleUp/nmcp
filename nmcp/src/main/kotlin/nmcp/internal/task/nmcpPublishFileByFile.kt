package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import nmcp.transport.FilesystemTransport
import nmcp.transport.HttpTransport
import nmcp.transport.NmcpCredentials
import nmcp.transport.publishFileByFile

@GTask(pure = false)
internal fun nmcpPublishFileByFile(
    logger: GLogger,
    url: String,
    username: String?,
    password: String?,
    inputFiles: GInputFiles,
) {
    val credentials = if (username != null) {
        check(!password.isNullOrBlank()) {
            "Nmcp: password is missing"
        }
        NmcpCredentials(username, password)
    } else {
        null
    }

    val transport = when {
        url.startsWith("http://") || url.startsWith("https://") -> {
            HttpTransport(url, credentials, logger)
        }
        url.startsWith("file://") -> {
            FilesystemTransport(url.substring("file://".length))
        }
        else -> {
            error("Nmcp: unsupported url '$url'")
        }
    }

    publishFileByFile(transport, inputFiles)
}
