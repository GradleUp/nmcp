package nmcp

import gratatouille.GPlugin
import nmcp.internal.DefaultNmcpAggregationExtension
import nmcp.internal.DefaultNmcpExtension
import org.gradle.api.Project

@GPlugin(id = "com.gradleup.nmcp")
fun nmcp(project: Project) {
    project.extensions.create(NmcpExtension::class.java, "nmcp", DefaultNmcpExtension::class.java, project)
}

@GPlugin(id = "com.gradleup.nmcp.aggregation")
fun nmcpAggregation(project: Project) {
    project.extensions.create(NmcpAggregationExtension::class.java, "nmcpAggregation", DefaultNmcpAggregationExtension::class.java, project)
}
