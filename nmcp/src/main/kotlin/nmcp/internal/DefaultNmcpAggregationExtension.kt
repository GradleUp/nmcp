package nmcp.internal

import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import org.gradle.api.Action
import org.gradle.api.Project

private abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    override val allFiles = consumerConfiguration.incoming.artifactView { it.lenient(true) }.files

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        project.registerPublishToCentralPortalTasks(
            name = "aggregation",
            inputFiles = allFiles,
            action = action
        )
    }

    override fun publishAllProjectsProbablyBreakingProjectIsolation() {
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
