package nmcp.internal.task

import gratatouille.FileWithPath
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GTask
import java.security.MessageDigest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nmcp.internal.ArtifactMetadata
import nmcp.internal.FilesystemTransport
import nmcp.internal.Gav
import nmcp.internal.HttpTransport
import nmcp.internal.Transport
import nmcp.internal.VersionMetadata
import nmcp.internal.filterFiles
import nmcp.internal.put
import nmcp.internal.replaceBuildNumber
import nmcp.internal.toPath
import nmcp.internal.xml
import okio.ByteString.Companion.toByteString

@GTask(pure = false)
fun nmcpPublishFileByFile(
    logger: GLogger,
    url: String,
    username: String?,
    password: String?,
    inputFiles: GInputFiles,
) {
    val credentials = if (username != null) {
        check(!password.isNullOrBlank()) {
            "Ncmp: password is missing"
        }
        nmcp.internal.Credentials(username, password)
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

    inputFiles
        .filterFiles()
        .groupBy {
            it.normalizedPath.substringBeforeLast('/')
        }.forEach { (gavPath, files) ->
            val gav = Gav.from(gavPath)
            val version = gav.version

            if (files.all { it.normalizedPath.substringAfterLast('/').startsWith("maven-metadata") }) {
                /**
                 * Update the [artifact metadata](https://maven.apache.org/repositories/metadata.html).
                 *
                 * See https://repo1.maven.org/maven2/com/apollographql/apollo/apollo-api-jvm/maven-metadata.xml for an example.
                 */
                val localArtifactMetadataFile = files.firstOrNull {it.normalizedPath.substringAfterLast('/') == "maven-metadata.xml" }
                if (localArtifactMetadataFile == null) {
                    error("Nmcp: cannot find artifact maven-metadata.xml in '${gav.groupId.toPath()}/${gav.artifactId}'")
                }
                val artifactMetadataPath = localArtifactMetadataFile.normalizedPath

                val localArtifactMetadata = xml.decodeFromString<ArtifactMetadata>(localArtifactMetadataFile.file.readText())
                val remoteArtifactMetadata = transport.get(artifactMetadataPath)

                val existingVersions = if (remoteArtifactMetadata != null) {
                    xml.decodeFromString<ArtifactMetadata>(remoteArtifactMetadata.use { it.readUtf8() }).versioning.versions
                } else {
                    emptyList()
                }

                /**
                 * See https://github.com/gradle/gradle/blob/cb0c615fb8e3690971bb7f89ad80f58943360624/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/AbstractMavenPublisher.java#L116.
                 */
                val versions = existingVersions.toMutableList()
                if (!versions.none { it == gav.version }) {
                    versions.add(gav.version)
                }
                val newArtifactMetadata = localArtifactMetadata.copy(
                    versioning = localArtifactMetadata.versioning.copy(
                        versions = versions,
                    ),
                )

                val bytes = encodeToXml(newArtifactMetadata).toByteArray()
                transport.put(artifactMetadataPath, bytes)
                setOf("md5", "sha1", "sha256", "sha512").forEach {
                    transport.put("$artifactMetadataPath.$it", bytes.digest(it.uppercase()))
                }

                return@forEach
            }
            /**
             * This is a proper directory containing artifacts
             */
            if (version.endsWith("-SNAPSHOT")) {
                /**
                 * This is a snapshot:
                 * - update the [version metadata](https://maven.apache.org/repositories/metadata.html).
                 * - path the file names to include the new build number.
                 *
                 * See https://s01.oss.sonatype.org/content/repositories/snapshots/com/apollographql/apollo/apollo-api-jvm/maven-metadata.xml for an example.
                 *
                 * For snapshots, it's not 100% clear who owns the metadata as the repository might expire some snapshot and therefore need to rewrite the
                 * metadata to keep things consistent. This means, there are 2 possibly concurrent writers to maven-metadata.xml: the repository and the
                 * publisher. Hopefully it's not too much of a problem in practice.
                 *
                 * See https://github.com/gradle/gradle/blob/d1ee068b1ee7f62ffcbb549352469307781af72e/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/MavenRemotePublisher.java#L70.
                 */
                val versionMetadataPath = "$gavPath/maven-metadata.xml"
                val localVersionMetadataFile = files.firstOrNull {
                    it.normalizedPath == versionMetadataPath
                }
                if (localVersionMetadataFile == null) {
                    error("Nmcp: cannot find version maven-metadata.xml in '$gavPath'")
                }

                val localVersionMetadata =
                    xml.decodeFromString<VersionMetadata>(localVersionMetadataFile.file.readText())
                val remoteVersionMetadata = transport.get(versionMetadataPath)

                val buildNumber = if (remoteVersionMetadata == null) {
                    1
                } else {
                    xml.decodeFromString<VersionMetadata>(remoteVersionMetadata.use { it.readUtf8() }).versioning.snapshot.buildNumber + 1
                }

                val newVersionMetadata = localVersionMetadata.copy(
                    versioning = localVersionMetadata.versioning.copy(
                        snapshot = localVersionMetadata.versioning.snapshot.copy(buildNumber = buildNumber),
                    ),
                )

                val renamedFiles = files.mapNotNull {
                    if (it.file.name.startsWith("maven-metadata.xml")) {
                        return@mapNotNull null
                    }
                    val newName = it.file.name.replaceBuildNumber(gav.artifactId, gav.version, buildNumber)
                    FileWithPath(it.file, "$gavPath/$newName")
                }

                transport.uploadFiles(renamedFiles)

                val bytes = encodeToXml(newVersionMetadata).toByteArray()
                transport.put(versionMetadataPath, bytes)
                setOf("md5", "sha1", "sha256", "sha512").forEach {
                    transport.put("$versionMetadataPath.$it", bytes.digest(it.uppercase()))
                }
            } else {
                /**
                 * Not a snapshot, plainly update all the files
                 */
                transport.uploadFiles(files)
            }
        }
}

private fun Transport.uploadFiles(filesWithPath: List<FileWithPath>) {
    filesWithPath.forEach {
        put(it.normalizedPath, it.file)
    }
}

/**
 * Helper function to add the `<?xml...` preamble as I haven't found how to do it with xmlutils
 */
internal inline fun <reified T> encodeToXml(t: T): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml.encodeToString(t)
}

internal fun String.removeDot(): String = replace(".", "")

private fun ByteArray.digest(name: String): String {
    val md = MessageDigest.getInstance(name)

    md.update(this, 0, size)
    val digest = md.digest()

    return digest.toByteString().hex()
}
