package nmcp.transport

import gratatouille.tasks.FileWithPath
import gratatouille.tasks.GInputFiles
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nmcp.internal.task.ArtifactMetadata
import nmcp.internal.task.Gav
import nmcp.internal.task.VersionMetadata
import nmcp.internal.task.replaceBuildNumber
import nmcp.internal.task.xml
import okio.ByteString.Companion.toByteString

fun publishFileByFile(
    transport: Transport,
    inputFiles: GInputFiles,
) {
    val allFiles = inputFiles.filter { it.file.isFile }
    val gavPaths = allFiles.filter { it.normalizedPath.endsWith(".pom") || it.normalizedPath.endsWith(".module") }
        .map { it.normalizedPath.substringBeforeLast('/') }
        .distinct()

    val lastUpdated = timestampNow()

    gavPaths.forEach { gavPath ->
        val gav = Gav.from(gavPath)
        val version = gav.version
        val gavFiles = allFiles.filter { it.normalizedPath.startsWith(gavPath) }

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
             * metadata to keep things consistent. This means there are 2 possibly concurrent writers to maven-metadata.xml: the repository and the
             * publisher. Hopefully, it's not too much of a problem in practice.
             *
             * See https://github.com/gradle/gradle/blob/d1ee068b1ee7f62ffcbb549352469307781af72e/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/MavenRemotePublisher.java#L70.
             */
            val versionMetadataPath = "$gavPath/maven-metadata.xml"
            val localVersionMetadataFile = gavFiles.firstOrNull {
                it.normalizedPath == versionMetadataPath
            }
            val localVersionMetadata = if (localVersionMetadataFile != null) {
                xml.decodeFromString<VersionMetadata>(localVersionMetadataFile.file.readText())
            } else {
                VersionMetadata(
                    groupId = gav.groupId,
                    artifactId = gav.artifactId,
                    version = gav.version,
                    versioning = VersionMetadata.Versioning(
                        snapshot = VersionMetadata.Snapshot(timestamp = lastUpdated, buildNumber = 1),
                        lastUpdated = lastUpdated,
                        snapshotVersions = emptyList()
                    )
                )
            }

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

            val renamedFiles = gavFiles.mapNotNull {
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
            transport.uploadFiles(gavFiles)
        }

        /**
         * Update the [artifact metadata](https://maven.apache.org/repositories/metadata.html).
         *
         * See https://repo1.maven.org/maven2/com/apollographql/apollo/apollo-api-jvm/maven-metadata.xml for an example.
         */
        val index = gavPath.lastIndexOf('/')
        check (index != -1) {
            "Nmcp: invalid gav path: '$gavPath'"
        }
        val artifactMetadataPath = "${gavPath.substring(0, index)}/maven-metadata.xml"
        val localArtifactMetadataFile = allFiles.firstOrNull { it.normalizedPath == artifactMetadataPath }
        val localArtifactMetadata = if (localArtifactMetadataFile == null) {
            // The publisher did not artifact level metadata, let's
            ArtifactMetadata(
                groupId = gav.groupId,
                artifactId = gav.artifactId,
                versioning = ArtifactMetadata.Versioning(
                    latest = gav.version,
                    release = gav.version,
                    versions = emptyList(),
                    lastUpdated = lastUpdated,
                )
            )
        } else {
            xml.decodeFromString<ArtifactMetadata>(localArtifactMetadataFile.file.readText())
        }

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

    }
}


private fun Transport.uploadFiles(filesWithPath: List<FileWithPath>) {
    filesWithPath.sortedBy { it.normalizedPath }.forEach {
        put(it.normalizedPath, it.file)
    }
}

internal fun timestampNow(): String {
    val now = Instant.now().atZone(java.time.ZoneOffset.UTC)

    return String.format("%04d%02d%02d%02d%02d%02d", now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute, now.second)
}

/**
 * Helper function to add the `<?xml...` preamble as I haven't found how to do it with xmlutils
 */
internal inline fun <reified T> encodeToXml(t: T): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml.encodeToString(t)
}

private fun ByteArray.digest(name: String): String {
    val md = MessageDigest.getInstance(name)

    md.update(this, 0, size)
    val digest = md.digest()

    return digest.toByteString().hex()
}
