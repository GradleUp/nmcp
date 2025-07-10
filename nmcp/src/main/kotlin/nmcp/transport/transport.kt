package nmcp.transport

import java.io.File
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
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
) : Transport {
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

