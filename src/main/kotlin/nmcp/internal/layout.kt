package nmcp.internal

internal data class Gav(
    val groupId: String,
    val artifactId: String,
    val version: String,
) {
    companion object {
        fun from(gavPath: String): Gav {
            val versionIndex = gavPath.lastIndexOf('/')
            check(versionIndex != -1) {
                "Nmcp: invalid maven path '$gavPath' (expected group/artifact/version)"
            }
            val version = gavPath.substring(versionIndex + 1)
            val artifactIndex = gavPath.lastIndexOf('/', versionIndex - 1)
            check(artifactIndex != -1) {
                "Nmcp: invalid maven path '$gavPath' (expected group/artifact/version)"
            }
            val artifact = gavPath.substring(artifactIndex + 1, versionIndex)
            val group = gavPath.substring(0, artifactIndex)

            check(group.isNotEmpty()) {
                "Nmcp: empty groupId in '$gavPath'"
            }
            check(artifact.isNotEmpty()) {
                "Nmcp: empty artifactId in '$gavPath'"
            }
            check(version.isNotEmpty()) {
                "Nmcp: empty version in '$gavPath'"
            }
            return Gav(group.toGroupId(), artifact, version)
        }
    }
}

internal class ArtifactExtra(
    val classifier: String?,
    val extension: String
) {
    val fullClassifier: String
        get() {
            return if (classifier == null) {
                ""
            } else {
                "-$classifier"
            }
        }
    companion object {
        /**
         * ${artifactId}-${version}-${classifier}.${extension}
         */
        fun from(fileName: String, artifactId: String, version: String): ArtifactExtra {
            val prefix = "${artifactId}-${version}"
            check(fileName.startsWith("${artifactId}-${version}")) {
                "Nmcp: Artifact '$fileName' doesn't start with '${artifactId}-${version}'"
            }
            when (fileName.get(prefix.length)) {
                '-' -> {
                    val extensionStart = fileName.indexOf('.', startIndex = prefix.length + 1)
                    check (extensionStart != -1) {
                        "Nmcp: Artifact '$fileName' is missing an extension"
                    }
                    return ArtifactExtra(
                        classifier = fileName.substring(prefix.length + 1, extensionStart),
                        extension = fileName.substring(extensionStart + 1)
                    )
                }
                '.' -> {
                    return ArtifactExtra(
                        classifier = null,
                        extension = fileName.substring(prefix.length + 1)
                    )
                }
                else -> error("Nmcp: invalid artifact: '$fileName', expected a classifier or an extension, found '${fileName.substring(prefix.length)}'.")
            }
        }
    }
}

internal fun String.toPath() = this.replace('.', '/')
internal fun String.toGroupId() = this.replace('/', '.')
