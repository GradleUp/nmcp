package nmcp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension

class NmcpPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.add("nmcp", NmcpExtension(target))
    }
}