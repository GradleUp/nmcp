pluginManagement {
  includeBuild("../../")
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
    }
  }
}

include(":module1", ":module2")