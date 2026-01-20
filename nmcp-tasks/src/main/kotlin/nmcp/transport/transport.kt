package nmcp.transport

import gratatouille.tasks.GLogger
import java.io.File
import kotlin.math.pow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException
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

        val request = Request.Builder()
            .get()
            .url(url)
            .maybeAddAuthorization(getAuthorization)
            .build()

        val result = executeWithRetries(logger, client, request)
        if (result is HttpError && result.code == 404) {
            return null
        }
        if (result !is Success) {
            error("Nmcp: cannot GET '$url' (${result})")
        }

        return result.body
    }

    override fun put(path: String, body: Content) {
        val url = baseUrl.newBuilder()
            .addPathSegments(path)
            .build()

        logger.info("Nmcp: put '$url'")

        val request =  Request.Builder()
            .put(body.toRequestBody())
            .url(url)
            .maybeAddAuthorization(putAuthorization)
            .build()

        val result = executeWithRetries(logger, client, request)
        if (result is Success) {
            result.body.close()
            return
        }

        val error = buildString {
            appendLine("Nmcp: cannot PUT '$url'")
            appendLine("$result")
            if (result is HttpError) {
                when (result.code) {
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
        error(error)
    }
}

/**
 * In some cases, 401 is actually retryable.
 * This is the case for:
 * - PUT on htps://central.sonatype.com/repository/maven-snapshots/
 * - verification of a deployment
 *
 * This is quite unexpected, and we code defensively here to be robust to those cases.
 * We also retry other errors.
 *
 * Example of transient 401:
 * ```
 * Execution failed for task ':nmcpPublishAggregationToCentralPortal'.
 * > A failure occurred while executing nmcp.internal.task.NmcpPublishWithPublisherApiWorkAction
 *    > Cannot verify deployment fbed2636-e25d-4538-be7d-7693d475595d status (HTTP status='401'): {"error":{"message":"Invalid token"}}
 * ```
 *
 * TODO:
 * - rework this to not block the thread.
 * - move the logic to some upper, sonatype-only layer
 * - fine tune the retry logic. Do we want to retry everything like we do here? Or are some HTTP errors actually
 * not retryable?
 *
 * @return the result. If the result is a success, the caller MUST close its body.
 */
internal fun executeWithRetries(logger: GLogger, client: OkHttpClient, request: Request): Result {
    var attempt = 0
    val attemptCount = 3
    while(true) {
        val result = executeInternal(client, request)
        if (result is Success) {
            return result
        }
        if (result is HttpError && result.code == 404) {
            // 404 is not retryable
            return result
        }
        if (attempt == attemptCount - 1) {
            return result
        }

        logger.lifecycle("Nmcp: put '${request.url}' failed (${result}), retrying... (attempt ${attempt + 1}/${attemptCount})")
        Thread.sleep(2.0.pow(attempt.toDouble()).toLong() * 1_000)
        attempt++
    }
}

internal fun executeInternal(client: OkHttpClient, request: Request): Result {
    return try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            return Success(response.body.source())
        }

        HttpError(response.code, response.body.string())
    } catch (e: IOException) {
        NetworkError(e)
    }
}

internal sealed interface Result
internal class NetworkError(val exception: IOException) : Result {
    override fun toString(): String {
        return "NetworkError: ${exception.message}"
    }
}
internal class HttpError(val code: Int, val body: String): Result {
    override fun toString(): String {
        return "HTTP error $code: '$body'"
    }
}
internal class Success(val body: BufferedSource) : Result

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

