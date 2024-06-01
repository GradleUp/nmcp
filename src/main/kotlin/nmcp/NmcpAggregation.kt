package nmcp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class NmcpAggregation @Inject constructor(
    private val configuration: Configuration,
    private val project: Project,
    ) {
    abstract val username: Property<String>
    abstract val password: Property<String>
    abstract val publicationType: Property<String>
    abstract val publicationName: Property<String>
    abstract val endpoint: Property<String>

    fun project(path: String) {
        project.dependencies.add(configuration.name, project.dependencies.project(mapOf("path" to  path)))
    }
}