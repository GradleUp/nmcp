package nmcp.internal.task

import gratatouille.FileWithPath
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GTask
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nmcp.internal.ArtifactExtra
import nmcp.internal.ArtifactMetadata
import nmcp.internal.FilesystemTransport
import nmcp.internal.Gav
import nmcp.internal.HttpTransport
import nmcp.internal.Transport
import nmcp.internal.VersionMetadata
import nmcp.internal.put
import nmcp.internal.toPath
import nmcp.internal.xml

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
            val now = currentTimeUtcAsTimestamp()

            val isSnapshot: Boolean
            if (version.endsWith("-SNAPSHOT")) {
                /**
                 * This is a snapshot:
                 * - rename the files to include the build number and timestamp.
                 * - also write the [version metadata](https://maven.apache.org/repositories/metadata.html).
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
                val existingVersionMetadata = transport.get(versionMetadataPath)
                isSnapshot = true

                val buildNumber = if (existingVersionMetadata == null) {
                    1
                } else {
                    xml.decodeFromString<VersionMetadata>(existingVersionMetadata.use { it.readUtf8() }).versioning.snapshot.buildNumber + 1
                }

                val snapshotVersions = mutableListOf<VersionMetadata.SnapshotVersion>()
                val renamedFiles = mutableListOf<FileWithPath>()

                files.forEach {
                    val name = it.normalizedPath.substringAfterLast('/')
                    val extra = ArtifactExtra.from(name, gav.artifactId, gav.version)

                    val newVersion = "${gav.version.replace("-SNAPSHOT", "")}-$now-$buildNumber"
                    renamedFiles.add(
                        FileWithPath(
                            it.file,
                            "$gavPath/${gav.artifactId}-$newVersion${extra.fullClassifier}.${extra.extension}",
                        ),
                    )
                    when (extra.extension) {
                        ".asc",
                        ".md5",
                        ".sha1",
                        ".sha256",
                        ".sha512",
                            -> {
                            // This is not a "true" artifact, ignore it
                            Unit
                        }
                        else -> {
                            snapshotVersions.add(
                                VersionMetadata.SnapshotVersion(
                                    classifier = extra.classifier,
                                    extension = extra.extension,
                                    value = newVersion,
                                    updated = now.removeDot(),
                                ),
                            )
                        }
                    }
                }

                val newVersionMetadata = VersionMetadata(
                    groupId = gav.groupId,
                    artifactId = gav.artifactId,
                    versioning = VersionMetadata.Versioning(
                        lastUpdated = now.removeDot(),
                        snapshot = VersionMetadata.Snapshot(
                            timestamp = now,
                            buildNumber = buildNumber,
                        ),
                        snapshotVersions = snapshotVersions,
                    ),
                    version = gav.version,
                )

                transport.uploadFiles(renamedFiles)
                transport.put(versionMetadataPath, encodeToXml(newVersionMetadata))
            } else {
                /**
                 * Not a snapshot, plainly update all the files
                 */
                isSnapshot = false
                transport.uploadFiles(files)
            }

            /**
             * In all cases, update the [artifact metadata](https://maven.apache.org/repositories/metadata.html).
             *
             * See https://repo1.maven.org/maven2/com/apollographql/apollo/apollo-api-jvm/maven-metadata.xml for an example.
             */
            val artifactMetadataPath = "${gav.groupId.toPath()}/${gav.artifactId}/maven-metadata.xml"
            val existingArtifactMetadata = transport.get(artifactMetadataPath)

            val existingVersions = if (existingArtifactMetadata != null) {
                xml.decodeFromString<ArtifactMetadata>(existingArtifactMetadata.use { it.readUtf8() }).versioning.versions
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
            val newArtifactMetadata = ArtifactMetadata(
                groupId = gav.groupId,
                artifactId = gav.artifactId,
                versioning = ArtifactMetadata.Versioning(
                    latest = gav.version,
                    /**
                     * Always use the latest version as the release version.
                     * This is technically not correct as the latest version might be an older release from a branch.
                     * But the versioning sorting rules are not super well-defined so this aims for simplicity.
                     * Gradle implementation [here](https://github.com/gradle/gradle/blob/cb0c615fb8e3690971bb7f89ad80f58943360624/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/AbstractMavenPublisher.java#L121)
                     */
                    release = if (isSnapshot) "" else gav.version,
                    versions = versions,
                    lastUpdated = now.removeDot(),
                ),
            )
            transport.put(artifactMetadataPath, encodeToXml(newArtifactMetadata))
        }
}

private fun Transport.uploadFiles(filesWithPath: List<FileWithPath>) {
    filesWithPath.forEach {
        put(it.normalizedPath, it.file)
    }
}

internal fun currentTimeUtcAsTimestamp(): String {
    // The timestamp is expressed using UTC in the format yyyyMMdd.HHmmss.
    // https://maven.apache.org/ref/3.9.10/maven-repository-metadata/repository-metadata.html
    val datetime = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    return String.format(
        "%04d%02d%02d.%02d%02d%02d",
        datetime.year,
        datetime.monthNumber,
        datetime.dayOfMonth,
        datetime.hour,
        datetime.minute,
        datetime.second,
    )
}

/**
 * Helper function to add the `<?xml...` preamble as I haven't found how to do it with xmlutils
 */
internal inline fun <reified T> encodeToXml(t: T): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml.encodeToString(t)
}

internal fun String.removeDot(): String = replace(".", "")
