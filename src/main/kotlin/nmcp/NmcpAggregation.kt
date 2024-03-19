package nmcp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property

class NmcpAggregation(
    private val configuration: Configuration,
    private val project: Project,
    val username: Property<String>,
    val password: Property<String>,
    val publicationType: Property<String>,
    val publicationName: Property<String>,
    ) {
    fun project(path: String) {
        project.dependencies.add(configuration.name, project.dependencies.project(mapOf("path" to  path)))
    }
}