package nmcp.internal

import gratatouille.wiring.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.internal.task.registerNmcpPublishFileByFileTask
import org.gradle.api.Action
import org.gradle.api.Project

abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    private val lenientFiles = consumerConfiguration.incoming.artifactView { it.lenient(true) }.files

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        val centralPortalOptions = project.createCentralPortalOptions(action)

        project.registerPublishToCentralPortalTasks(
            name = "aggregation",
            inputFiles = lenientFiles,
            spec = centralPortalOptions
        )
    }

    override fun publishAllProjectsProbablyBreakingProjectIsolation() {
        check(project === project.rootProject) {
            "publishAllProjectsProbablyBreakingProjectIsolation() must be called from root project"
        }

        project.subprojects { aproject ->
            aproject.pluginManager.withPlugin("maven-publish") {
                aproject.pluginManager.apply("com.gradleup.nmcp")

                consumerConfiguration.dependencies.add(aproject.dependencies.create(aproject))
            }
        }
    }
}
