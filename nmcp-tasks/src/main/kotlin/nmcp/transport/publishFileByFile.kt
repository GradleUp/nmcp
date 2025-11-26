package nmcp.transport

import gratatouille.tasks.FileWithPath
import gratatouille.tasks.GInputFiles
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nmcp.internal.task.ArtifactMetadata
import nmcp.internal.task.Gav
import nmcp.internal.task.VersionMetadata
import nmcp.internal.task.Artifact
import nmcp.internal.task.xml
import okio.ByteString.Companion.toByteString

internal val defaultParallelism = 8

fun publishFileByFile(
    transport: Transport,
    inputFiles: GInputFiles,
) {
    return publishFileByFile(transport, inputFiles, defaultParallelism)
}

/**
 * Publishes to a local repository.
 *
 * [publishFileByFile] computes the group and version maven-metadata.xml files.
 *
 * @param from a directory containing the files to publish as a m2 layout without maven-metadata.xml.
 * @param into the destination directory.
 */
fun publishFileByFile(
    from: File,
    into: File,
) {
    return publishFileByFile(
        FilesystemTransport(into.absolutePath),
        from.walk().filter { it.isFile }.map {
            FileWithPath(it, it.relativeTo(from).path)
        }.toList(),
        defaultParallelism,
    )
}

fun publishFileByFile(
    transport: Transport,
    inputFiles: GInputFiles,
    parallelism: Int,
) {
    val allFiles = inputFiles.filter { it.file.isFile }
    val gavPaths = allFiles.filter { it.normalizedPath.endsWith(".pom") || it.normalizedPath.endsWith(".module") }
        .map { it.normalizedPath.substringBeforeLast('/') }
        .distinct()

    val lastUpdated = Instant.now()

    runBlocking {
        withContext(Dispatchers.IO.limitedParallelism(parallelism)) {
            gavPaths.map {
                launch {
                    publishGav(it, allFiles, lastUpdated, transport)
                }
            }.joinAll()
        }
    }
}

private fun publishGav(
    gavPath: String,
    allFiles: List<FileWithPath>,
    lastUpdated: Instant,
    transport: Transport,
) {
    val gav = Gav.from(gavPath)
    val version = gav.baseVersion
    val gavFiles = allFiles.filter { it.normalizedPath.startsWith(gavPath) }

    /**
     * This is a proper directory containing artifacts
     */
    if (version.endsWith("-SNAPSHOT")) {
        /**
         * This is a snapshot:
         * - update the [version metadata](https://maven.apache.org/repositories/metadata.html).
         * - patch the file names to include the new build number.
         *
         * For snapshots, it's not 100% clear who owns the metadata as the repository might expire some snapshot and therefore need to rewrite the
         * metadata to keep things consistent. This means there are 2 possibly concurrent writers to maven-metadata.xml: the repository and the
         * publisher. Hopefully, it's not too much of a problem in practice.
         *
         * See https://github.com/gradle/gradle/blob/d1ee068b1ee7f62ffcbb549352469307781af72e/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/MavenRemotePublisher.java#L70.
         *
         */
        val versionMetadataPath = "$gavPath/maven-metadata.xml"
        val remoteVersionMetadata = transport.get(versionMetadataPath)
        val buildNumber = if (remoteVersionMetadata == null) {
            1
        } else {
            xml.decodeFromString<VersionMetadata>(remoteVersionMetadata.use { it.readUtf8() }).versioning.snapshot.buildNumber + 1
        }

        val renamedFiles = mutableListOf<FileWithPath>()
        val snapshotVersions = mutableListOf<VersionMetadata.SnapshotVersion>()

        gavFiles.forEach {
            if (it.file.name.startsWith("maven-metadata.xml")) {
                return@forEach
            }
            val artifact = Artifact.from(it.file.name, gav.artifactId, gav.baseVersion)
            val newVersion =
                "${gav.baseVersion.removeSuffix("-SNAPSHOT")}-${lastUpdated.asTimestamp(true)}-$buildNumber"
            val newArtifact = artifact.copy(version = newVersion)
            val newName = newArtifact.fileName()
            renamedFiles.add(FileWithPath(it.file, "$gavPath/${newName}"))

            if (newArtifact.extension.substringAfterLast('.') !in checksums) {
                snapshotVersions.add(
                    VersionMetadata.SnapshotVersion(
                        classifier = newArtifact.classifier,
                        extension = newArtifact.extension,
                        value = newArtifact.version,
                        updated = lastUpdated.asTimestamp(false),
                    ),
                )
            }
        }

        val versionMetadata =
            VersionMetadata(
                groupId = gav.groupId,
                artifactId = gav.artifactId,
                version = gav.baseVersion,
                versioning = VersionMetadata.Versioning(
                    snapshot = VersionMetadata.Snapshot(
                        timestamp = lastUpdated.asTimestamp(true),
                        buildNumber = buildNumber,
                    ),
                    lastUpdated = lastUpdated.asTimestamp(false),
                    snapshotVersions = snapshotVersions,
                ),
            )


        transport.uploadFiles(renamedFiles)

        val bytes = encodeToXml(versionMetadata).toByteArray()
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
     * See https://repo1.maven.org/maven2/com/apollographql/apollo/apollo-api-jvm/maven-metadata.xml for an example of artifact metadata.
     */
    val index = gavPath.lastIndexOf('/')
    check(index != -1) {
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
                latest = gav.baseVersion,
                release = gav.baseVersion,
                versions = emptyList(),
                lastUpdated = lastUpdated.asTimestamp(false),
            ),
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
     * Make sure there is always at least one version.
     * This is needed at least for things like https://shields.io, probably other things too.
     * See https://github.com/gradle/gradle/blob/cb0c615fb8e3690971bb7f89ad80f58943360624/platforms/software/maven/src/main/java/org/gradle/api/publish/maven/internal/publisher/AbstractMavenPublisher.java#L116.
     */
    val versions = existingVersions.toMutableList()
    if (versions.none { it == gav.baseVersion }) {
        versions.add(gav.baseVersion)
    }
    val newArtifactMetadata = localArtifactMetadata.copy(
        versioning = localArtifactMetadata.versioning.copy(
            versions = versions,
        ),
    )

    val bytes = encodeToXml(newArtifactMetadata).toByteArray()
    transport.put(artifactMetadataPath, bytes)
    checksums.forEach {
        transport.put("$artifactMetadataPath.$it", bytes.digest(it.uppercase()))
    }
}


private fun Transport.uploadFiles(filesWithPath: List<FileWithPath>) {
    filesWithPath.sortedBy { it.normalizedPath }.forEach {
        put(it.normalizedPath, it.file)
    }
}

internal fun Instant.asTimestamp(withDot: Boolean): String {
    val now = this.atZone(ZoneOffset.UTC)

    val dot = if (withDot) "." else ""
    return String.format(
        "%04d%02d%02d${dot}%02d%02d%02d",
        now.year,
        now.monthValue,
        now.dayOfMonth,
        now.hour,
        now.minute,
        now.second,
    )
}

internal val checksums = setOf("md5", "sha1", "sha256", "sha512")

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
