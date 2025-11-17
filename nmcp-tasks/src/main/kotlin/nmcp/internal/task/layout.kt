package nmcp.internal.task

/**
 * See https://maven.apache.org/repositories/layout.html
 */
internal data class Gav(
    val groupId: String,
    val artifactId: String,
    val baseVersion: String,
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

private fun String.classifierAndExtension(index: Int): Pair<String?, String> {
    check(this.isNotEmpty()) {
        "Nmcp: Premature end of artifact name, cannot find extension"
    }
    val name = this
    val dashOrDot = index
    val dot: Int
    val classifier = if (name.get(dashOrDot) == '-') {
        dot = name.indexOfFirst { it == '.' }
        check(dashOrDot >= 0) {
            "Nmcp: No extension found in '$this'"
        }
        name.substring(dashOrDot + 1, dot)
    } else {
        check(name.get(dashOrDot) == '.') {
            "Nmcp: No extension found in '$this'"
        }
        dot = dashOrDot
        null
    }

    return classifier to name.substring(dot + 1)
}

internal data class Artifact(
    val artifactId: String,
    val version: String,
    val classifier: String?,
    val extension: String,
) {
    constructor(artifactId: String, version: String, pair: Pair<String?, String>) : this(
        artifactId,
        version,
        pair.first,
        pair.second,
    )

    fun fileName(): String {
        return buildString {
            append(artifactId)
            append('-')
            append(version)
            if (classifier != null) {
                append('-')
                append(classifier)
            }
            append('.')
            append(extension)
        }
    }

    companion object {
        fun from(name: String, artifactId: String, baseVersion: String): Artifact {
            return if (!baseVersion.endsWith("-SNAPSHOT")) {
                /**
                 * SNAPSHOT
                 */
                check(name.startsWith("$artifactId-$baseVersion")) {
                    "Nmcp: '$this' does not start with '$artifactId-$baseVersion' and is not a SNAPSHOT"
                }

                val dashOrDot = "$artifactId-$baseVersion".length

                Artifact(
                    artifactId, baseVersion, name.classifierAndExtension(dashOrDot),
                )
            } else {
                /**
                 * SNAPSHOT, name should look like "module1-0.0.3-20250623.104441-1.jar.asc"
                 */
                val versionWithoutSnapshot = baseVersion.removeSuffix("-SNAPSHOT")
                check(name.startsWith("$artifactId-$versionWithoutSnapshot")) {
                    "Nmcp: '$this' is a SNAPSHOT and should start with '$artifactId-$versionWithoutSnapshot'"
                }
                val regex = Regex("(-[0-9]{8}\\.[0-9]{6}-[0-9]+)(.*)")
                val matchResult = regex.matchAt(name, "$artifactId-$versionWithoutSnapshot".length)
                check(matchResult != null) {
                    "Nmcp: '$this' doesn't match ${regex.pattern}"
                }

                val ce = matchResult.groupValues.get(2)

                Artifact(
                    artifactId,
                    "$baseVersion${matchResult.groupValues.get(1)}",
                    ce.classifierAndExtension(0),
                )
            }
        }
    }
}

internal fun String.toPath() = this.replace('.', '/')
internal fun String.toGroupId() = this.replace('/', '.')
