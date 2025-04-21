package nmcp.internal

import gratatouille.GPlugin
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.Project

@GPlugin(id = "com.gradleup.nmcp")
fun nmcp(target: Project) {
    target.extensions.create("nmcp", NmcpExtension::class.java, target)
}

@GPlugin(id = "com.gradleup.nmcp.aggregation")
fun nmcpAggregation(target: Project) {
    target.extensions.create("nmcpAggregation", NmcpAggregationExtension::class.java, target)
}