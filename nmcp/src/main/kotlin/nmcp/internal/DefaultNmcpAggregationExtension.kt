package nmcp.internal

import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

internal abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {
    private var centralPortalConfigured = false

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    override val allFiles: FileCollection = consumerConfiguration.incoming.artifactView { it.lenient(true) }.files.asFileTree

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        check(!centralPortalConfigured) {
            "Nmcp: centralPortal {} must be called only once"
        }
        centralPortalConfigured = true

        project.registerPublishToCentralPortalTasks(
            kind = Kind.aggregation,
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
