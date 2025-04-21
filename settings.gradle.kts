pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      maven("https://storage.googleapis.com/gradleup/m2")
    }
  }
}
