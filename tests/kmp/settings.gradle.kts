pluginManagement {
    includeBuild("../../")
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
        }
    }
}

includeBuild("build-logic")
include(":module1")
include(":module2")