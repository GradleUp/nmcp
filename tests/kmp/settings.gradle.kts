pluginManagement {
    includeBuild("../../")
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
        }
    }
}

plugins {
    id("com.gradleup.nmcp.settings")
}

includeBuild("../../")
include(":module1")
include(":module2")

