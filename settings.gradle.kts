import org.gradle.internal.impldep.kotlinx.serialization.json.JsonNull.content

pluginManagement {
    listOf(
        repositories,
        dependencyResolutionManagement.repositories,
    ).forEach {
        it.apply {
            mavenCentral()
            maven("https://storage.googleapis.com/gradleup/m2") {
                content {
                    // those dependencies are only used at build time, and is safe to fetch as a snapshot
                    includeModule("com.gradleup.gratatouille", "gratatouille-processor")
                    includeModule("com.gradleup.tapmoc", "tapmoc-tasks")
                    includeModule("com.gradleup.nmcp", "nmcp-tasks")
                }
            }
            google {
                content {
                    includeGroup("com.android.application")
                    includeGroup("com.android.tools.build")
                    includeGroup("com.android")
                }
            }
        }
    }
    repositories {
        maven("https://storage.googleapis.com/gradleup/m2")
    }
}

rootProject.name = "nmcp-root"

include(":nmcp", ":nmcp-tasks")

