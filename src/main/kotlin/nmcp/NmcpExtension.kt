package nmcp

import gratatouille.GExtension
import gratatouille.capitalizeFirstLetter
import nmcp.internal.configureAttributes
import nmcp.internal.nmcpProducerConfigurationName
import nmcp.internal.task.registerPublishReleaseTask
import nmcp.internal.task.registerPublishSnapshotTask
import nmcp.internal.withRequiredPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Zip

@GExtension(pluginId = "com.gradleup.nmcp")
open class NmcpExtension(private val project: Project) {
    internal val spec = project.objects.newInstance(CentralPortalOptions::class.java)
    // Lifecycle tasks to publish all the publications in the given project
    private val publishAllPublicationsToCentralPortal = project.tasks.register("publishAllPublicationsToCentralPortal")
    private val publishAllPublicationsToCentralSnapshots = project.tasks.register("publishAllPublicationsToCentralSnapshots")

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
            publishing.publications.configureEach {
                registerInternal(it.name)
            }
        }
    }

    /**
     * This creates a new repository for each publication so that the publications do not overlap each other
     * and can use an isolated directory.
     *
     * See https://github.com/GradleUp/nmcp/issues/34#issuecomment-2827704768
     */
    private fun registerInternal(publicationName: String) {
        val capitalized = publicationName.capitalizeFirstLetter()

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val m2Dir = project.layout.buildDirectory.dir("nmcp/m2$capitalized")
        val repoName = "nmcp$capitalized"
        publishing.apply {
            repositories.apply {
                maven {
                    it.name = repoName
                    it.url = project.uri(m2Dir)
                }
            }
        }

        val publication = publishing.publications.findByName(publicationName)
        if (publication == null) {
            val candidates = publishing.publications.map { it.name }
            error("Nmcp: cannot find publication '$publicationName'. Candidates are: '${candidates.joinToString()}'")
        }

        val publishToNmcpTaskProvider = project.tasks.named("publish${capitalized}PublicationTo${repoName.capitalizeFirstLetter()}Repository")

        publishToNmcpTaskProvider.configure {
            it.doFirst {
                m2Dir.get().asFile.apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
        }
        val zipTaskProvider = project.tasks.register("zip${capitalized}Publication", Zip::class.java) {
            it.dependsOn(publishToNmcpTaskProvider)
            it.from(m2Dir)
            it.eachFile {
                // Exclude maven-metadata files, or the bundle is not recognized
                // See https://slack-chats.kotlinlang.org/t/16407246/anyone-tried-the-https-central-sonatype-org-publish-publish-#c8738fe5-8051-4f64-809f-ca67a645216e
                if (it.name.startsWith("maven-metadata")) {
                    it.exclude()
                }
            }
            it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
            it.archiveFileName.set("publication$capitalized.zip")
        }

        val artifactId = if (publication is MavenPublication) {
            project.provider { publication.artifactId }
        } else {
            project.provider { "${project.name}"}
        }
        val publishRelease = project.registerPublishReleaseTask(
            taskName = "publish${capitalized}PublicationToCentralPortal",
            inputFile = zipTaskProvider.flatMap { it.archiveFile },
            artifactId = artifactId,
            spec = spec
        )
        val publishSnapshots = project.registerPublishSnapshotTask(
            taskName = "publish${capitalized}PublicationToCentralSnapshots",
            inputFile = zipTaskProvider.flatMap { it.archiveFile },
            username = spec.username,
            password = spec.password,
            version = project.provider { "${project.version}" },
        )

        publishAllPublicationsToCentralSnapshots.configure {
            it.dependsOn(publishSnapshots)
        }
        publishAllPublicationsToCentralPortal.configure {
            it.dependsOn((publishRelease))
        }

        project.artifacts.add(nmcpProducerConfigurationName, zipTaskProvider)
    }

    /**
     * Configures the central portal parameters
     */
    fun centralPortal(action: Action<CentralPortalOptions>) {
        action.execute(spec)
    }
}
