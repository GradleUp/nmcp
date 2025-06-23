package nmcp.internal.task

import gratatouille.GInputFiles
import gratatouille.GOutputFile
import gratatouille.GTask

@GTask
fun nmcpGuessComponents(
    inputFiles: GInputFiles,
    outfileFile: GOutputFile,
) {

    val gavs = inputFiles.mapNotNull {
        if (!it.normalizedPath.endsWith(".pom")) {
            return@mapNotNull null
        }

        Gav.from(it.normalizedPath.substringBeforeLast('/'))
    }

    val groups = gavs.map { it.groupId }.distinct()
    val artifacts = gavs.map { it.artifactId }.distinct()
    val versions = gavs.map { it.version }.distinct()

    val name = buildString {
        if (groups.size == 1) {
            append(groups.single())
        } else {
            append("multiple-groups")
        }
        append(':')
        if (artifacts.size == 1) {
            append(artifacts.single())
        } else {
            append("multiple-artifacts")
        }
        append(':')
        if (versions.size == 1) {
            append(versions.single())
        } else {
            append("multiple-versions")
        }
    }

    outfileFile.writeText(name)
}
