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


@GTask
internal fun nmcpFindDeploymentName(inputFiles: GInputFiles, outputFile: GOutputFile) {
  val groups = linkedSetOf<String>()
  val artifacts = linkedSetOf<String>()
  val versions = linkedSetOf<String>()

  for (file in inputFiles) {
    if (!file.normalizedPath.endsWith(".pom")) {
      continue
    }

    val gav = Gav.from(file.normalizedPath.substringBeforeLast('/'))
    groups.add(gav.groupId)
    artifacts.add(gav.artifactId)
    versions.add(gav.baseVersion)
  }

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

private fun Set<String>.toDisplayName(): String {
    if (size == 1) {
        return single()
    }
    return "${longestCommonPrefix(toList())}*"
}
