// Change this to fix the build
rootProject.name = "duplicate-name"
pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    it.maven("../../../build/m2")
  }
}

include(":duplicate-name")
