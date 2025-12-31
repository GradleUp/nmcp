package nmcp.internal

import gratatouille.GExtension
import gratatouille.capitalizeFirstLetter
import java.io.File
import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.LocalRepositoryOptions
import nmcp.internal.task.registerNmcpPublishFileByFileToFileSystemTask
import nmcp.nmcpAggregationExtensionName
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

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

        project.afterEvaluate {
            val allNames = mutableSetOf<String>()
            project.allprojects {
                check (!allNames.contains(it.name.lowercase())) {
                    "Nmcp: duplicate project name: '${it.name}'. This is usually resolved by setting your root project name in your settings.gradle[.kts] file: `rootProject.name = \"\${someUniqueName}\". " +
                        "See https://github.com/gradle/gradle/issues/36167 for more details"
                }
                allNames.add(it.name.lowercase())
            }

            if (!allowEmptyAggregation.orElse(false).get()) {
                check(consumerConfiguration.dependencies.isNotEmpty()) {
                    "Nmcp: the aggregation is empty. This is usually a misconfiguration. If this is intentional, set `allowEmptyAggregation` to true."
                }
            }
        }
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(spec)
    }

    override fun localRepository(action: Action<LocalRepositoryOptions>) {
        val options = project.objects.newInstance(LocalRepositoryOptions::class.java)
        action.execute(options)
        project.registerNmcpPublishFileByFileToFileSystemTask(
            taskName = "nmcpPublishAggregationTo${options.name.get().capitalizeFirstLetter()}Repository",
            inputFiles = allFiles,
            m2AbsolutePath = project.provider { project.file(options.path.get()).absolutePath },
            parallelism = project.provider { 1 },
        )
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

    abstract override val allowEmptyAggregation: Property<Boolean>
}

private fun isCompatible(artifactResult: ArtifactResult): Boolean {
    if (artifactResult !is ResolvedArtifactResult) {
        return false
    }
    val usage = artifactResult.variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)
    return usage?.name == usageValue
}
