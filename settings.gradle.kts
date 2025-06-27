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
        exclusiveContent {
            forRepository { maven("https://storage.googleapis.com/gradleup/m2") }
            filter {
                includeGroup("com.gradleup.librarian")
            }
        }
    }
}

rootProject.name = "nmcp-root"

include(":nmcp")
