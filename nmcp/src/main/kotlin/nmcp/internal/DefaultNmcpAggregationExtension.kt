package nmcp.internal

import gratatouille.GExtension
import gratatouille.GPlugin
import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.nmcpAggregationExtensionName
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty

@GExtension(
    pluginId = "com.gradleup.nmcp.aggregation",
    publicType = NmcpAggregationExtension::class,
    extensionName = nmcpAggregationExtensionName,
)
internal abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {
    private val spec = project.objects.newInstance(CentralPortalOptions::class.java)

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    override val allFiles: ConfigurableFileCollection = project.files()

    init {
        allFiles.from(
            consumerConfiguration
                .incoming
                .artifactView { it.lenient(true) }
                .artifacts
                .resolvedArtifacts
                .map {
                    it.filter(::isCompatible).map { it.file }
                },
        )
        project.registerPublishToCentralPortalTasks(
            kind = Kind.aggregation,
            inputFiles = allFiles,
            spec = spec,
        )
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(spec)
    }

    @Deprecated("Use the settings plugin or a convention plugin instead")
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

private fun isCompatible(artifactResult: ArtifactResult): Boolean {
    if (artifactResult !is ResolvedArtifactResult) {
        return false
    }
    val usage = artifactResult.variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)
    return usage?.name == usageValue
}
