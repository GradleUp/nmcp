package nmcp

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

internal val configurationName = "nmcpProducer"
internal val attribute = "com.gradleup.nmcp"
internal val attributeValue = "bundle"
internal val usageValue = "nmcp"

internal fun HasConfigurableAttributes<*>.configureAttributes(project: Project) {
    attributes {
        it.attribute(Attribute.of(attribute, Named::class.java), project.objects.named(Named::class.java, attributeValue))
        it.attribute(USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usageValue))
    }
}

class NmcpPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.configurations.create(configurationName) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false

            it.configureAttributes(target)
        }
        target.extensions.add("nmcp", NmcpExtension(target))
    }
}