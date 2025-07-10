package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import nmcp.transport.Content
import nmcp.transport.Transport
import nmcp.transport.nmcpClient
import nmcp.transport.publishFileByFile
import nmcp.transport.toRequestBody
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okio.BufferedSource
import okio.use

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

    val transport = CentralSnapshotsTransport(
        baseUrl = snapshotsUrl,
        getAuthorization = null,
        putAuthorization = authorizationHeader,
        logger = logger,
    )

    logger.lifecycle("Nmcp: uploading files to $snapshotsUrl")
    publishFileByFile(transport, inputFiles)
}

internal class CentralSnapshotsTransport(
    baseUrl: String,
    private val getAuthorization: String?,
    private val putAuthorization: String?,
    private val logger: GLogger,
) : Transport {
    private val client = nmcpClient.newBuilder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            builder.addHeader("Accept", "application/json")
            builder.addHeader("User-Agent", "nmcp")
            chain.proceed(builder.build())
        }
        .build()

    private val baseUrl = baseUrl.toHttpUrl()

    override fun get(path: String): BufferedSource? {
        val url = baseUrl.newBuilder()
            .addPathSegments(path)
            .build()

        logger.info("Nmcp: get '$url'")

        val response = Request.Builder()
            .get()
            .url(url)
            .maybeAddAuthorization(getAuthorization)
            .build()
            .let {
                client.newCall(it).execute()
            }

        if (response.code == 404) {
            return null
        }
        check(response.isSuccessful) {
            "Nmcp: cannot GET '$url' (statusCode=${response.code}):\n${response.body!!.string()}"
        }

        return response.body!!.source()
    }

    override fun put(path: String, body: Content) {
        val url = baseUrl.newBuilder()
            .addPathSegments(path)
            .build()

        logger.info("Nmcp: put '$url'")

        Request.Builder()
            .put(body.toRequestBody())
            .url(url)
            .maybeAddAuthorization(putAuthorization)
            .build()
            .let {
                client.newCall(it).execute()
            }.use { response ->
                check(response.isSuccessful) {
                    val extra = if (response.code == 400) {
                        " - Did you enable the snapshots in the Central UI?"
                    } else {
                        ""
                    }
                    "Nmcp: cannot PUT '$url' (statusCode=${response.code}$extra):\n'${response.body!!.string()}'"
                }
            }
    }
}

private fun Request.Builder.maybeAddAuthorization(authorization: String?) = apply {
    if (authorization != null) {
        addHeader("Authorization", authorization)
    }
}
