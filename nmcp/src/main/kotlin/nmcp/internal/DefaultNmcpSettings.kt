package nmcp.internal

import gratatouille.GExtension
import javax.inject.Inject
import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.NmcpSettings
import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory

@Suppress("UnstableApiUsage")
@GExtension(pluginId = "com.gradleup.nmcp.settings", extensionName = "nmcpSettings", publicType = NmcpSettings::class)
abstract class DefaultNmcpSettings(settings: Settings): NmcpSettings {
    @get:Inject
    abstract val objects: ObjectFactory

    private val spec = objects.newInstance(CentralPortalOptions::class.java)

    init {
        settings.gradle.lifecycle.beforeProject { project ->
            if (project.rootProject == project) {
                project.pluginManager.apply("com.gradleup.nmcp.aggregation")

                project.extensions.getByType(NmcpAggregationExtension::class.java).centralPortal {
                    it.username.set(spec.username)
                    it.password.set(spec.password)
                    it.publishingType.set(spec.publishingType)
                    it.publicationName.set(spec.publicationName)
                    it.validationTimeout.set(spec.validationTimeout)
                    it.publishingTimeout.set(spec.publishingTimeout)
                    it.baseUrl.set(spec.baseUrl)
                    it.uploadSnapshotsParallelism.set(spec.uploadSnapshotsParallelism)
                }

                project.allprojects {
                    project.dependencies.add(nmcpConsumerConfigurationName, project.dependencies.create(it))
                }
            }
            project.pluginManager.withPlugin("maven-publish") {
                project.pluginManager.apply("com.gradleup.nmcp")
            }
        }
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(spec)
    }
}
