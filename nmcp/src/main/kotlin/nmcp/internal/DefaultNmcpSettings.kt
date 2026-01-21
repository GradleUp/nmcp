package nmcp.internal

import gratatouille.GExtension
import javax.inject.Inject
import nmcp.CentralPortalOptions
import nmcp.MavenPublishOptions
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import nmcp.NmcpSettings
import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory

@Suppress("UnstableApiUsage")
@GExtension(pluginId = "com.gradleup.nmcp.settings", extensionName = "nmcpSettings", publicType = NmcpSettings::class)
abstract class DefaultNmcpSettings(settings: Settings): NmcpSettings {
    @get:Inject
    abstract val objects: ObjectFactory

    private val centralPortalOptions = objects.newInstance(CentralPortalOptions::class.java)
    private val mavenPublishOptions = MavenPublishOptions()

    init {
        settings.gradle.lifecycle.beforeProject { project ->
            if (project.rootProject == project) {
                project.pluginManager.apply("com.gradleup.nmcp.aggregation")

                project.extensions.getByType(NmcpAggregationExtension::class.java).centralPortal {
                    it.username.set(centralPortalOptions.username)
                    it.password.set(centralPortalOptions.password)
                    it.publishingType.set(centralPortalOptions.publishingType)
                    it.publicationName.set(centralPortalOptions.publicationName)
                    it.validationTimeout.set(centralPortalOptions.validationTimeout)
                    it.publishingTimeout.set(centralPortalOptions.publishingTimeout)
                    it.baseUrl.set(centralPortalOptions.baseUrl)
                    it.uploadSnapshotsParallelism.set(centralPortalOptions.uploadSnapshotsParallelism)
                }

                project.allprojects {
                    project.dependencies.add(nmcpConsumerConfigurationName, project.dependencies.create(it))
                }
            }
            if (mavenPublishOptions.groupId != null) {
                // Apply Nmcp unconditionally
                project.pluginManager.apply("com.gradleup.nmcp")
                project.extensions.getByType(NmcpExtension::class.java).mavenPublish {
                    it.groupId = mavenPublishOptions.groupId
                    it.version = mavenPublishOptions.version

                    it.description = mavenPublishOptions.description
                    it.vcsUrl = mavenPublishOptions.vcsUrl
                    it.developer = mavenPublishOptions.developer
                    it.spdxId = mavenPublishOptions.spdxId
                }
            } else {
                // Only apply Nmcp for projects that actually publish something
                project.pluginManager.withPlugin("maven-publish") {
                    project.pluginManager.apply("com.gradleup.nmcp")
                }
            }
        }
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(centralPortalOptions)
    }

    override fun mavenPublish(action: Action<MavenPublishOptions>) {
        action.execute(mavenPublishOptions)
    }
}
