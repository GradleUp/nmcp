package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import kotlin.collections.distinct
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.single
import kotlin.text.endsWith
import kotlin.text.substringBeforeLast


internal fun GInputFiles.findDeploymentName(): String {
  val gavs = mapNotNull {
    if (!it.normalizedPath.endsWith(".pom")) {
      return@mapNotNull null
    }

    Gav.from(it.normalizedPath.substringBeforeLast('/'))
  }

  val groups = gavs.map { it.groupId }.distinct()
  val artifacts = gavs.map { it.artifactId }.distinct()
  val versions = gavs.map { it.version }.distinct()

  return buildString {
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
}
