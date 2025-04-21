package nmcp

import gratatouille.GExtension
import nmcp.internal.configureAttributes
import nmcp.internal.nmcpConsumerConfigurationName
import nmcp.internal.task.registerPublishTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

@GExtension(pluginId = "com.gradleup.nmcp.aggregation")
open class NmcpAggregationExtension(private val project: Project) {
    internal val spec = project.objects.newInstance(NmcpSpec::class.java)

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    val zipTaskProvider = project.tasks.register("zipAggregationPublication", Zip::class.java) {
        it.archiveFileName.set("publicationAggregated.zip")
        it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
        it.from(consumerConfiguration.elements.map {
            check (it.isNotEmpty()) {
                "nmcp: aggregation is empty. Specify projects to publish by adding them to the 'nmcpAggregation' configuration."
            }
            it.map {
                project.zipTree(it)
            }
        })
    }

    init {
        project.registerPublishTask(
            taskName = "publishAggregatedPublicationToCentralPortal",
            inputFile = zipTaskProvider.flatMap { it.archiveFile },
            spec = spec
        )
    }

    /**
     * Configures the central portal parameters
     */
    fun centralPortal(action: Action<NmcpSpec>) {
        action.execute(spec)
    }

    /**
     * Applies the `com.gradleup.nmcp` plugin to every project that also applies `maven-publish` and adds the
     * `publishAggregatedPublicationToCentralPortal` task to publish a bundle containing all projects.
     *
     * This is breaking project isolation
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


