package nmcp.internal.task

import gratatouille.GInputFile
import gratatouille.GLogger
import gratatouille.GTask
import java.util.zip.ZipInputStream
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.source

@GTask(pure = false)
fun publishSnapshot(
    logger: GLogger,
    username: String?,
    password: String?,
    version: String,
    inputFile: GInputFile,
) {
    check(!username.isNullOrBlank()) {
        "Ncmp: username is missing"
    }
    check(!password.isNullOrBlank()) {
        "Ncmp: password is missing"
    }
    check(version.endsWith("-SNAPSHOT")) {
        "Ncmp: Cannot publish snapshot version '$version' without -SNAPSHOT suffix"
    }

    val okHttpClient = client.newBuilder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            builder.addHeader("Authorization", Credentials.basic(username, password))
            builder.addHeader("Accept", "application/json")
            builder.addHeader("Content-Type", "application/json")
            builder.addHeader("User-Agent", "vespene")
            chain.proceed(builder.build())
        }.build()

    ZipInputStream(inputFile.inputStream()).use {
        while (true) {
            val entry = it.nextEntry ?: break
            if (entry.isDirectory) continue
            val relativePath = entry.name
            logger.debug("Nmcp: uploading $relativePath...")

            val url = "https://central.sonatype.com/repository/maven-snapshots/$relativePath"
            val request = Request.Builder()
                .put(it.source().buffer().readByteArray().toRequestBody("application/octet-stream".toMediaType()))
                .url(url)
                .build()

            val uploadResponse = okHttpClient.newCall(request).execute()
            check(uploadResponse.isSuccessful) {
                "Cannot put $url:\n${uploadResponse.body?.string()}"
            }
        }
    }
}
