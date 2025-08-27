pluginManagement {
    listOf(
        repositories,
        dependencyResolutionManagement.repositories,
    ).forEach {
        it.apply {
            mavenCentral()
        }
    }
    repositories {
        maven("https://storage.googleapis.com/gradleup/m2")
    }
}

rootProject.name = "nmcp-root"

include(":nmcp")
