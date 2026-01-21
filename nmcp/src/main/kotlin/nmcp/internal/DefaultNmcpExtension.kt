package nmcp.internal

import com.android.build.gradle.internal.crash.afterEvaluate
import gratatouille.GExtension
import gratatouille.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.MavenPublishOptions
import nmcp.NmcpExtension
import nmcp.internal.task.registerCleanupDirectoryTask
import nmcp.nmcpExtensionName
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

@GExtension(pluginId = "com.gradleup.nmcp", publicType = NmcpExtension::class, extensionName = nmcpExtensionName)
internal abstract class DefaultNmcpExtension(private val project: Project) : NmcpExtension {
    private val m2Dir = project.layout.buildDirectory.file("nmcp/m2")
    private val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
    private val mavenPublishOptions = MavenPublishOptions()

    // TODO: it's not clear whether we need that while we could simply resolve the project artifacts
    private val m2Files = project.files()
    private val cleanupRepository =
        project.registerCleanupDirectoryTask(directory = m2Dir.map { it.asFile.absolutePath })

    init {
        project.configurations.create(nmcpProducerConfigurationName) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            // See https://github.com/GradleUp/nmcp/issues/2
            it.isVisible = false

            it.configureAttributes(project)
        }

        project.withRequiredPlugin("maven-publish") {
            val publishing = project.extensions.getByType(PublishingExtension::class.java)

            publishing.repositories.apply {
                maven {
                    it.name = "nmcp"
                    it.url = project.uri(m2Dir)
                }
            }

            val publishAllToNmcpTaskProvider = project.tasks.named("publishAllPublicationsToNmcpRepository")
            publishAllToNmcpTaskProvider.configure {
                /**
                 * Hide the task from `./gradlew tasks`
                 */
                it.group = null
            }
            project.artifacts.add(nmcpProducerConfigurationName, m2Dir) {
                it.builtBy(publishAllToNmcpTaskProvider)
            }

            m2Files.builtBy(publishAllToNmcpTaskProvider)
            m2Files.from(project.fileTree(m2Dir))

            publishing.publications.configureEach { publication ->
                if (publication is MavenPublication) {
                    val capitalized = publication.name.capitalizeFirstLetter()
                    val publishToNmcpTaskProvider =
                        project.tasks.named("publish${capitalized}PublicationToNmcpRepository")
                    publishToNmcpTaskProvider.configure {
                        /**
                         * m2Dir is shared between multiple tasks. Those tasks are considered an implementation detail though, and it's considered
                         * an error for the user to invoke them manually.
                         *
                         * As long as the individual tasks are trigger by `publishAllPublicationsTo${Foo}` then this sharing shouldn't be too
                         * much of an issue, and having a single directory greatly simplifies the Gradle model and the output of `./gradlew tasks --all`.
                         */
                        it.group = null
                        it.dependsOn(cleanupRepository)
                        // See https://github.com/GradleUp/nmcp/issues/216#issuecomment-3655649387
                        it.outputs.upToDateWhen { false }
                    }
                }
            }
            project.registerPublishToCentralPortalTasks(
                kind = Kind.allPublications,
                inputFiles = m2Files,
                spec = centralPortalOptions,
                allowEmptyFiles = project.provider { false },
            )
        }
    }

    override fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>) {
        action.execute(centralPortalOptions)
    }

    override fun extraFiles(files: Any) {
        project.artifacts.add(nmcpProducerConfigurationName, files)
        m2Files.from(files)
    }

    override fun mavenPublish(action: Action<MavenPublishOptions>) {
        project.plugins.apply("maven-publish")
        val publishing = project.extensions.getByType(PublishingExtension::class.java)

        action.execute(mavenPublishOptions)
        project.createMissingPublications(publishing.publications)
        afterEvaluate {
            project.configurePom(publishing.publications, mavenPublishOptions)
        }
    }
}
