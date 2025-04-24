pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            maven("https://storage.googleapis.com/gradleup/m2")
        }
    }
}

includeBuild("../../")
includeBuild("build-logic")
include(":module1")
include(":module2")