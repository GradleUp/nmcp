package nmcp.internal.task

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.ByteString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration


@DisableCachingByDefault
internal abstract class NmcpPublishTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val verifyStatus: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val verificationTimeout: Property<Duration>

    @get:Input
    @get:Optional
    abstract val publicationType: Property<String>

    @get:Input
    abstract val publicationName: Property<String>

    @get:Input
    @get:Optional
    abstract val endpoint: Property<String>

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
                publicationName.get(),
                inputFile.get().asFile.asRequestBody("application/zip".toMediaType())
            )
            .build()

        val publicationType = publicationType.orElse("USER_MANAGED").get()

        val endpoint = endpoint.getOrElse("https://central.sonatype.com/api/v1/")

        val deploymentId = Request.Builder()
            .post(body)
            .addHeader("Authorization", "UserToken $token")
            .url(endpoint + "publisher/upload?publishingType=$publicationType")
            .build()
            .let {
                client.newCall(it).execute()
            }.use {
                if (!it.isSuccessful) {
                    error("Cannot deploy to maven central (status='${it.code}'): ${it.body?.string()}")
                }

                it.body!!.string()
            }

        logger.lifecycle("Nmcp: deployment bundle '$deploymentId' uploaded.")

        if (verifyStatus.orElse(true).get()) {
            logger.lifecycle("Nmcp: verifying deployment status...")
            val timeout = verificationTimeout.orElse(10.minutes.toJavaDuration()).get()
            val mark = markNow()
            while (true) {
                check (mark.elapsedNow() < timeout.toKotlinDuration()) {
                    "Nmcp: timeout while verifying deployment status."
                }
                when (val status = verifyStatus(
                    deploymentId = deploymentId,
                    endpoint = endpoint,
                    token = token,
                )) {
                    PENDING,
                    VALIDATING,
                    PUBLISHING -> {
                        // Come back later
                        Thread.sleep(2000)
                    }

                    VALIDATED -> {
                        logger.lifecycle("Deployment has passed validation, publish it manually from the Central Portal UI.")
                        break
                    }

                    PUBLISHED -> {
                        logger.lifecycle("Deployment is published.")
                        break
                    }

                    is FAILED -> {
                        error("Deployment has failed:\n${status.error}")
                    }
                }
            }
        }
    }
}

private sealed interface Status

// A deployment is uploaded and waiting for processing by the validation service
private object PENDING : Status

// A deployment is being processed by the validation service
private object VALIDATING : Status

// A deployment has passed validation and is waiting on a user to manually publish via the Central Portal UI
private object VALIDATED : Status

// A deployment has been either automatically or manually published and is being uploaded to Maven Central
private object PUBLISHING : Status

// A deployment has successfully been uploaded to Maven Central
private object PUBLISHED : Status

// A deployment has encountered an error
private class FAILED(val error: String) : Status

private val client = OkHttpClient.Builder().build()

private fun verifyStatus(
    deploymentId: String,
    endpoint: String,
    token: String,
): Status {
    Request.Builder()
        .post(ByteString.EMPTY.toRequestBody())
        .addHeader("Authorization", "UserToken $token")
        .url(endpoint + "publisher/status?id=$deploymentId")
        .build()
        .let {
            client.newCall(it).execute()
        }.use {
            if (!it.isSuccessful) {
                error("Cannot verify deployment status (HTTP status='${it.code}'): ${it.body?.string()}")
            }

            val str = it.body!!.string()
            val element = Json.parseToJsonElement(str)
            check(element is JsonObject) {
                "Nmcp: unexpected status response: $str"
            }

            val state = element["deploymentState"]
            check(state is JsonPrimitive && state.isString) {
                "Nmcp: unexpected status: $state"
            }

            return when (state.content) {
                "PENDING" -> PENDING
                "VALIDATING" -> VALIDATING
                "VALIDATED" -> VALIDATED
                "PUBLISHING" -> PUBLISHING
                "PUBLISHED" -> PUBLISHED
                "FAILED" -> {
                    FAILED(element["errors"].toString())
                }
                else -> error("Nmcp: unexpected status: $state")
            }
        }
}