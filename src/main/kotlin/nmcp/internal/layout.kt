package nmcp.internal

import kotlin.collections.get
import org.apache.tools.ant.taskdefs.BuildNumber

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

internal fun String.replaceBuildNumber(artifactId: String, snapshotVersion: String, newBuildNumber: Int): String {
    // module1-0.0.3-20250623.104441-1.jar.asc
    val versionWithoutSnapshot = snapshotVersion.replace("-SNAPSHOT","")
    return replace(Regex("(${artifactId}-$versionWithoutSnapshot-[0-9]{8}\\.[0-9]{6}-)[0-9]+(.*)")) {
        "${it.groupValues[1]}$newBuildNumber${it.groupValues[2]}"
    }
}

internal fun String.toPath() = this.replace('.', '/')
internal fun String.toGroupId() = this.replace('/', '.')
