pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
    }
  }
    includeBuild("../../")
}

includeBuild("../../")

plugins {
    id("com.gradleup.nmcp.settings").apply(false)
}
