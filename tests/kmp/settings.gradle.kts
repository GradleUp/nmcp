pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
        }
    }
}

includeBuild("../../")
includeBuild("build-logic")
include(":module1")
include(":module2")
