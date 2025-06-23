package nmcp.internal

import gratatouille.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.NmcpExtension
import nmcp.internal.task.KindAll
import nmcp.internal.task.KindSingle
import nmcp.internal.task.registerNmcpGuessComponentsTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

open class DefaultNmcpExtension(private val project: Project): NmcpExtension {
    private val publicationFiles = mutableMapOf<String?, ConfigurableFileCollection>()

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
                registerPublicationTasks(it.name)
            }
        }
    }

    private fun filesFor(publicationName: String?): ConfigurableFileCollection {
        return publicationFiles.getOrPut(publicationName) { project.files() }
    }

    /**
     * Create tasks that export the artifacts, signatures and checksums
     */
    private fun registerPublicationTasks(publicationName: String) {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)

        val publication = publishing.publications.findByName(publicationName)
        if (publication == null) {
            val candidates = publishing.publications.map { it.name }
            error("Nmcp: cannot find publication '$publicationName'. Candidates are: '${candidates.joinToString()}'")
        }

        check(publication is MavenPublication) {
            error("Nmcp only supports MavenPublication (found ${publication.javaClass.simpleName})")
        }
        val capitalized = publicationName.capitalizeFirstLetter()

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

        val publishToNmcpTaskProvider = project.tasks.named("publish${capitalized}PublicationTo${repoName.capitalizeFirstLetter()}Repository")

        publishToNmcpTaskProvider.configure {
            // This is mostly an internal task, hide it from `./gradlew --tasks`
            it.group = null
            it.doFirst {
                m2Dir.get().asFile.apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
        }

        val publishAllToNmcpTaskProvider = project.tasks.named("publishAllPublicationsTo${repoName.capitalizeFirstLetter()}Repository")
        publishAllToNmcpTaskProvider.configure {
            // This is mostly an internal task, hide it from `./gradlew --tasks`
            it.group = null
        }

        val m2Files = project.files()
        m2Files.builtBy(publishAllToNmcpTaskProvider)
        m2Files.from(project.fileTree(m2Dir))

        project.artifacts.add(nmcpProducerConfigurationName, m2Dir) {
            it.builtBy(publishToNmcpTaskProvider)
        }

        filesFor(publicationName).from(m2Files)

        filesFor(null).from(m2Files)
    }

    override fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>) {
        val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
        action.execute(centralPortalOptions)

        val guessVersion = project.registerNmcpGuessComponentsTask(
            inputFiles = filesFor(null)
        )
        project.registerPublishToCentralPortalTasks(
            deploymentKind = KindAll,
            inputFiles = filesFor(null),
            defaultDeploymentName = guessVersion.flatMap { it.outfileFile }.map { it.asFile.readText() },
            spec = centralPortalOptions
        )
    }

    override fun publishToCentralPortal(
        publicationName: String,
        action: Action<CentralPortalOptions>,
    ) {
        val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
        action.execute(centralPortalOptions)

        val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(publicationName) ?: error("Nmcp: Cannot find publication '$publicationName'")
        publication as MavenPublication
        project.registerPublishToCentralPortalTasks(
            deploymentKind = KindSingle(publicationName),
            inputFiles = filesFor(publicationName),
            defaultDeploymentName = project.provider { "${publication.groupId}:${publication.artifactId}:${publication.version}" },
            spec = centralPortalOptions
        )
    }
}
