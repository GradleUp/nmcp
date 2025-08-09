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
        maven("https://storage.googleapis.com/gradleup/m2") {
            content {
                includeGroup("com.gradleup.librarian")
                includeGroup("com.gradleup.nmcp")
                includeGroup("com.gradleup.compat.patrouille")
            }
        }
    }
}

rootProject.name = "nmcp-root"

include(":nmcp")
