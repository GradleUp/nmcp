package nmcp

import gratatouille.GExtension
import javax.inject.Inject
import nmcp.internal.configureAttributes
import nmcp.internal.nmcpConsumerConfigurationName
import nmcp.internal.task.registerPublishTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.bundling.Zip

@GExtension(pluginId = "com.gradleup.nmcp.aggregation")
abstract class NmcpAggregationExtension(private val project: Project) {
    internal val spec = project.objects.newInstance(CentralPortalOptions::class.java)

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    init {
        val operations = archiveOperations
        val layout = project.layout
        val files= project.files(consumerConfiguration)

         val zipTaskProvider = project.tasks.register("zipAggregation", Zip::class.java) {
            it.archiveFileName.set("aggregation.zip")
            it.destinationDirectory.set(layout.buildDirectory.dir("nmcp/zip"))
            it.from(files.elements.map {
                check (it.isNotEmpty()) {
                    "nmcp: aggregation is empty. Make sure to create publications in sub-projects and specify projects to publish by adding them to the 'nmcpAggregation' configuration."
                }
                it.map {
                    operations.zipTree(it)
                }
            })
        }

        project.registerPublishTask(
            taskName = "publishAggregationToCentralPortal",
            inputFile = zipTaskProvider.flatMap { it.archiveFile },
            spec = spec
        )
    }

    /**
     * Configures the central portal parameters
     */
    fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(spec)
    }

    /**
     * Applies the `com.gradleup.nmcp` plugin to every project that also applies `maven-publish`.
     *
     * This function is not compatible with breaking project isolation. To be compatible with project isolation,
     * add each subproject to the `nmcpAggregation` configuration dependencies.
     */
    fun publishAllProjectsProbablyBreakingProjectIsolation() {
        check(project === project.rootProject) {
            "publishAllProjectsProbablyBreakingProjectIsolation() must be called from root project"
        }

        project.allprojects { aproject ->
            aproject.pluginManager.withPlugin("maven-publish") {
                aproject.pluginManager.apply("com.gradleup.nmcp")

                consumerConfiguration.dependencies.add(aproject.dependencies.create(aproject))
            }
        }
    }
}


