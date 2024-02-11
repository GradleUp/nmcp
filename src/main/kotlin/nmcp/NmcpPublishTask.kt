package nmcp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class NmcpPublishTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val publicationType: Property<String>

    @TaskAction
    fun taskAction() {
        val username = username.get()
        val password = password.get()

        check(username.isNotBlank()) {
            "Ncmp: username must not be empty"
        }
        check(password.isNotBlank()) {
            "Ncmp: password must not be empty"
        }

        val token = "$username:$password".let {
            Buffer().writeUtf8(it).readByteString().base64()
        }

        val body = MultipartBody.Builder()
            .addFormDataPart(
                "bundle",
                "publication.zip",
                inputFile.get().asFile.asRequestBody("application/zip".toMediaType())
            )
            .build()

        val publicationType = publicationType.orElse("USER_MANAGED").get()

        Request.Builder()
            .post(body)
            .addHeader("Authorization", "UserToken $token")
            .url("https://central.sonatype.com/api/v1/publisher/upload?publishingType=$publicationType")
            .build()
            .let {
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    })
                    .build()
                    .newCall(it).execute()
            }.use {
                if (!it.isSuccessful) {
                    error("Cannot publish to maven central (status='${it.code}'): ${it.body?.string()}")
                }
            }
    }

}