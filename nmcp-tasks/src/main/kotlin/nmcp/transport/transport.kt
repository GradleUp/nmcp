package nmcp.transport

import gratatouille.tasks.GLogger
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import okio.use

interface Transport {
    fun get(path: String): BufferedSource?
    fun put(path: String, body: Content)
}

internal fun Transport.put(path: String, body: String) {
    put(
        path,
        object : Content {
            override fun writeTo(sink: BufferedSink) {
                sink.writeUtf8(body)
            }
        },
    )
}

internal fun Transport.put(path: String, body: ByteArray) {
    put(
        path,
        object : Content {
            override fun writeTo(sink: BufferedSink) {
                sink.write(body)
            }
        },
    )
}

internal fun Transport.put(path: String, body: File) {
    put(
        path,
        object : Content {
            override fun writeTo(sink: BufferedSink) {
                body.source().use {
                    sink.writeAll(it)
                }
            }
        },
    )
}

interface Content {
    fun writeTo(sink: BufferedSink)
}

private fun Request.Builder.maybeAddAuthorization(authorization: String?) = apply {
    if (authorization != null) {
        addHeader("Authorization", authorization)
    }
}

internal class HttpTransport(
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
            response.close()
            return null
        }
        if (!response.isSuccessful) {
            response.close()
            error("Nmcp: cannot GET '$url' (statusCode=${response.code}):\n${response.body.string()}")
        }

        return response.body.source()
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
                    buildString {
                        appendLine("Nmcp: cannot PUT '$url' (statusCode=${response.code}).")
                        appendLine("Response body: '${response.body.string()}'")
                        when (response.code) {
                            400 -> {
                                appendLine("Things to double check:")
                                appendLine("Your artifacts have proper extensions (.jar, .pom, ...).")
                                appendLine("If publishing a XML file, the XML version is 1.0.")
                                appendLine("If publishing a snapshot, the artifacts version is ending with `-SNAPSHOT`.")
                            }
                            401 -> {
                                appendLine("Check your credentials")
                                appendLine("If publishing a snapshot, make sure you enabled snapshots on your namespace at https://central.sonatype.com/publishing/namespaces.")
                            }
                            403 -> {
                                appendLine("Check that you are publishing to the correct groupId.")
                            }
                            429 -> {
                                appendLine("Too many requests, try again later")
                            }
                        }
                    }
                }
            }
    }
}

fun Content.toRequestBody(): RequestBody {
    return object : RequestBody() {
        override fun contentType(): MediaType {
            return "application/octet-stream".toMediaType()
        }

        override fun writeTo(sink: BufferedSink) {
            this@toRequestBody.writeTo(sink)
        }
    }
}

internal class FilesystemTransport(
    private val basePath: String,
    private val logger: GLogger?,
) : Transport {
    override fun get(path: String): BufferedSource? {
        logger?.info("Nmcp: read '$path'")
        val file = File(basePath).resolve(path)
        if (!file.exists()) {
            return null
        }
        return file.source().buffer()
    }

    override fun put(path: String, body: Content) {
        logger?.info("Nmcp: write '$path'")
        File(basePath).resolve(path).apply {
            parentFile.mkdirs()
            sink().buffer().use {
                body.writeTo(it)
            }
        }
    }
}

