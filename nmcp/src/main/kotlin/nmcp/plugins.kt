package nmcp

import gratatouille.wiring.GPlugin
import nmcp.internal.DefaultNmcpAggregationExtension
import nmcp.internal.DefaultNmcpExtension
import nmcp.internal.nmcpConsumerConfigurationName
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

internal val nmcpExtensionName = "nmcp"
internal val nmcpAggregationExtensionName = "nmcpAggregation"

@GPlugin(id = "com.gradleup.nmcp")
internal fun nmcp(project: Project) {
    project.extensions.create(NmcpExtension::class.java, nmcpExtensionName, DefaultNmcpExtension::class.java, project)
}

@GPlugin(id = "com.gradleup.nmcp.aggregation")
internal fun nmcpAggregation(project: Project) {
    project.extensions.create(
        NmcpAggregationExtension::class.java,
        nmcpAggregationExtensionName,
        DefaultNmcpAggregationExtension::class.java,
        project,
    )
}

@Suppress("UnstableApiUsage")
@GPlugin(id = "com.gradleup.nmcp.settings")
internal fun nmcpSettings(settings: Settings) {
    settings.gradle.lifecycle.beforeProject { project ->
        if (project.rootProject == project) {
            project.pluginManager.apply("com.gradleup.nmcp.aggregation")

            project.allprojects {
                project.dependencies.add(nmcpConsumerConfigurationName, project.dependencies.create(it))
            }
        } else {
            project.pluginManager.withPlugin("maven-publish") {
                project.pluginManager.apply("com.gradleup.nmcp")
            }
        }
    }
}
