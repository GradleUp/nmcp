package nmcp

import gratatouille.GPlugin
import nmcp.internal.DefaultNmcpAggregationExtension
import nmcp.internal.DefaultNmcpExtension
import nmcp.internal.nmcpConsumerConfigurationName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

@GPlugin(id = "com.gradleup.nmcp")
fun nmcp(project: Project) {
    project.extensions.create(NmcpExtension::class.java, "nmcp", DefaultNmcpExtension::class.java, project)
}

@GPlugin(id = "com.gradleup.nmcp.aggregation")
fun nmcpAggregation(project: Project) {
    project.extensions.create(NmcpAggregationExtension::class.java, "nmcpAggregation", DefaultNmcpAggregationExtension::class.java, project)
}

@Suppress("UnstableApiUsage")
abstract class NmcpSettingsPlugin: Plugin<Settings> {
    override fun apply(target: Settings) {
        target.gradle.lifecycle.beforeProject { project ->
            if (project.rootProject == project) {
                project.pluginManager.apply("com.gradleup.nmcp.aggregation")

                project.subprojects {
                    project.dependencies.add(nmcpConsumerConfigurationName, project.dependencies.create(it))
                }
            } else {
                project.pluginManager.apply("com.gradleup.nmcp")
            }
        }
    }
}
