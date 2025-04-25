pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
    }
  }
}

includeBuild("../../")

include(":module1", ":module2")
