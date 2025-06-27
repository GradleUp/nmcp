package nmcp.transport

import gratatouille.tasks.GLogger
import java.io.File
import okhttp3.Credentials
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

interface Transport {
    fun get(path: String): BufferedSource?
    fun put(path: String, body: Content)
}

internal fun Transport.put(path: String, body: String) {
    put(path, object : Content {
        override fun writeTo(sink: BufferedSink) {
            sink.writeUtf8(body)
        }
    })
}

internal fun Transport.put(path: String, body: ByteArray) {
    put(path, object : Content {
        override fun writeTo(sink: BufferedSink) {
            sink.write(body)
        }
    })
}

internal fun Transport.put(path: String, body: File) {
    put(path, object : Content {
        override fun writeTo(sink: BufferedSink) {
            body.source().use {
                sink.writeAll(it)
            }
        }
    })
}

interface Content {
    fun writeTo(sink: BufferedSink)
}

internal class NmcpCredentials(val username: String, val password: String)

internal class HttpTransport(
    baseUrl: String,
    private val credentials: NmcpCredentials?,
    private val logger: GLogger,
) : Transport {
    private val client = nmcp.internal.task.nmcpClient.newBuilder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            if (credentials != null) {
                builder.addHeader(
                    "Authorization",
                    Credentials.basic(credentials.username, credentials.password),
                )
            }
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

        logger.lifecycle("Nmcp: get '$url'")

        val response = Request.Builder()
            .get()
            .url(url)
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

        logger.lifecycle("Nmcp: put '$url'")

        val response = Request.Builder()
            .put(body.toRequestBody())
            .url(url)
            .build()
            .let {
                client.newCall(it).execute()
            }

        check(response.isSuccessful) {
            "Nmcp: cannot PUT '$url' (statusCode=${response.code}):\n${response.body!!.string()}"
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
): Transport {
    override fun get(path: String): BufferedSource? {
        val file = File(basePath).resolve(path)
        if (!file.exists()) {
            return null
        }
        return file.source().buffer()
    }

    override fun put(path: String, body: Content) {
        File(basePath).resolve(path).apply {
            parentFile.mkdirs()
            sink().buffer().use {
                body.writeTo(it)
            }
        }
    }
}

