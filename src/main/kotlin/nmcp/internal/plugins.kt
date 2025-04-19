package nmcp.internal

import gratatouille.GPlugin
import nmcp.NmcpExtension
import org.gradle.api.Project

@GPlugin(id = "com.gradleup.nmcp")
fun apply(target: Project) {
    target.extensions.create("nmcp", NmcpExtension::class.java, target)
}
