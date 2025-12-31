pluginManagement {
    listOf(
        repositories,
        dependencyResolutionManagement.repositories,
    ).forEach {
        it.apply {
            mavenCentral()
            maven("https://storage.googleapis.com/gradleup/m2") {
                content {
                    // gratatouille-processor is only used at build time and is safe to fetch as a snapshot
                    includeModule("com.gradleup.gratatouille", "gratatouille-processor")
                    includeModule("com.gradleup.tapmoc", "tapmoc-tasks")
                }
            }
        }
    }
    repositories {
        maven("https://storage.googleapis.com/gradleup/m2")
    }
}

gradle.lifecycle.afterProject {
    extensions.getByType(PublishingExtension::class.java).repositories.maven {
        name = "test"
        url = uri("file://${rootProject.layout.buildDirectory.get().asFile.resolve("m2").absolutePath}")
    }
}
rootProject.name = "nmcp-root"

include(":nmcp", ":nmcp-tasks")

