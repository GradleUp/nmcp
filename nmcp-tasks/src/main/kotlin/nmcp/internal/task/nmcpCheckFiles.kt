package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask
import kotlin.collections.distinct
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.single
import kotlin.text.buildString
import kotlin.text.endsWith
import kotlin.text.substringBeforeLast


/**
 * Check the validity of the input files and outputs the name of the deployment
 */
@GTask
internal fun nmcpCheckFiles(inputFiles: GInputFiles, outputFile: GOutputFile, allowEmptyFiles: Boolean) {
    check(allowEmptyFiles || inputFiles.isNotEmpty()) {
        "Nmcp: there are no files to publish. Double check your configuration."
    }
    val gavs = inputFiles.mapNotNull {
    if (!it.normalizedPath.endsWith(".pom")) {
      return@mapNotNull null
    }

    Gav.from(it.normalizedPath.substringBeforeLast('/'))
  }

  val groups = gavs.map { it.groupId }.distinct()
  val artifacts = gavs.map { it.artifactId }.distinct()
  val versions = gavs.map { it.baseVersion }.distinct()

  val deploymentName = buildString {
      append(groups.toDisplayName())
      append(':')
      append(artifacts.toDisplayName())
      append(':')
      append(versions.toDisplayName())
  }
  outputFile.writeText(deploymentName)
}

private fun longestCommonPrefix(strings: List<String>): String {
    if (strings.isEmpty()) return ""

    // Start with the first string as the prefix
    var prefix = strings[0]

    for (i in 1 until strings.size) {
        val current = strings[i]
        var j = 0

        // Compare characters until they mismatch or end of one string
        while (j < prefix.length && j < current.length && prefix[j] == current[j]) {
            j++
        }

        // Trim the prefix to the matched part
        prefix = prefix.substring(0, j)

        // Early termination if there's no common prefix
        if (prefix.isEmpty()) return ""
    }

    return prefix
}

private fun List<String>.toDisplayName(): String {
    if (size == 1) {
        return single()
    }
    return "${longestCommonPrefix(this)}*"
}
