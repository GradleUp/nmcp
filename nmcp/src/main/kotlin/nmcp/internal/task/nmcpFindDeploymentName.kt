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
  val gavs = inputFiles.mapNotNull {
    if (!it.normalizedPath.endsWith(".pom")) {
      return@mapNotNull null
    }

    Gav.from(it.normalizedPath.substringBeforeLast('/'))
  }

  val groups = gavs.map { it.groupId }.distinct()
  val artifacts = gavs.map { it.artifactId }.distinct()
  val versions = gavs.map { it.version }.distinct()

  val deploymentName = buildString {
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
  outputFile.writeText(deploymentName)
}
