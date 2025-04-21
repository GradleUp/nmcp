pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      maven("https://storage.googleapis.com/gradleup/m2")
    }
  }
}

includeBuild("../../")

include(":module1", ":module2")