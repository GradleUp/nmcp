package nmcp.internal

import gratatouille.GPlugin
import nmcp.CentralPortalOptions
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension

@GPlugin("com.gradleup.nmcp.central-portal-options")
fun centralPortalOptions(target: Project) {
    target.pluginManager.apply("publishing")
    val publishing = target.extensions.getByType(PublishingExtension::class.java)
    (publishing as ExtensionAware).extensions.create("centralPortal", CentralPortalOptions::class.java)
}
