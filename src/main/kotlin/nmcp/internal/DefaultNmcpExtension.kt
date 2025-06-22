package nmcp.internal

import gratatouille.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.LocalRepositoryOptions
import nmcp.NmcpExtension
import nmcp.SigningOptions
import nmcp.internal.task.KindAll
import nmcp.internal.task.KindSingle
import nmcp.internal.task.registerNmcpGenerateChecksumsTask
import nmcp.internal.task.registerNmcpGeneratePublicationTask
import nmcp.internal.task.registerNmcpGenerateSignaturesTask
import nmcp.internal.task.registerNmcpPublishFileByFileTask
import nmcp.internal.task.registerPublishToCentralPortalTasks
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication

open class DefaultNmcpExtension(private val project: Project): NmcpExtension {
    private val signingOptions = project.objects.newInstance(SigningOptions::class.java)

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

        publishAllPublicationsToLocalRepository {
            it.name = "MavenLocal"
            it.directory = System.getenv("HOME") + "/.m2"
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

        check(publication is DefaultMavenPublication) {
            error("Nmcp only supports MavenPublication (found ${publication.javaClass.simpleName})")
        }
        val files = project.files()
        val classifiers = mutableListOf<String>()
        val extensions = mutableListOf<String>()
        publication.publishableArtifacts.forEach {
            files.from(it.file)
            files.builtBy(it.buildDependencies)
            /**
             * Gradle List properties do not allow null for some reason 🤷‍♂️
             * They fail with:
             *
             * ```
             * Cannot get the value of a property of type java.util.List with element type java.lang.String as the source value contains a null element.
             * ```
             *
             * So instead, we just pass the empty string.
             */
            classifiers.add(it.classifier ?: "")
            extensions.add(it.extension)
        }

        val generatePublication = project.registerNmcpGeneratePublicationTask(
            taskName = "nmcpGenerate${publicationName.capitalizeFirstLetter()}Publication",
            groupId = project.provider { publication.groupId },
            artifactId = project.provider { publication.artifactId },
            version = project.provider { publication.version },
            files = files,
            classifiers = project.objects.listProperty(String::class.java).apply { addAll(classifiers) },
            artifactExtensions = project.objects.listProperty(String::class.java).apply { addAll(extensions) },
        )

        project.artifacts.add(nmcpProducerConfigurationName, generatePublication)

        val generateSignatures = project.registerNmcpGenerateSignaturesTask(
            taskName = "nmcpGenerate${publicationName.capitalizeFirstLetter()}Signatures",
            inputFiles = project.files(generatePublication),
            signingKey = signingOptions.privateKey,
            signingKeyPassword = signingOptions.privateKeyPassword
        )

        val generateCheckSums = project.registerNmcpGenerateChecksumsTask(
            taskName = "nmcpGenerate${publicationName.capitalizeFirstLetter()}Checksums",
            inputFiles = project.files(generateSignatures, generatePublication),
        )

        filesFor(publicationName).from(generatePublication)
        filesFor(publicationName).from(generateSignatures)
        filesFor(publicationName).from(generateCheckSums)

        filesFor(null).from(generatePublication)
        filesFor(null).from(generateSignatures)
        filesFor(null).from(generateCheckSums)
    }

    override fun sign(action: Action<SigningOptions>) {
        action.execute(signingOptions)
    }

    override fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>) {
        val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
        action.execute(centralPortalOptions)

        project.registerPublishToCentralPortalTasks(
            deploymentKind = KindAll,
            inputFiles = filesFor(null),
            defaultDeploymentName = project.provider { "${project.group}:${project.name}:${project.version}" },
            spec = centralPortalOptions
        )
    }

    override fun publishAllPublicationsToLocalRepository(action: Action<LocalRepositoryOptions>) {
        val localRepositoryOptions = project.objects.newInstance(LocalRepositoryOptions::class.java)
        action.execute(localRepositoryOptions)

        val repoName = localRepositoryOptions.name ?: "LocalRepository"
        val taskName = "nmcpPublishAllPublicationsTo${repoName.capitalizeFirstLetter()}"
        project.registerNmcpPublishFileByFileTask(
            taskName = taskName,
            username = project.objects.property(String::class.java),
            password = project.objects.property(String::class.java),
            url = project.provider { "file://" + localRepositoryOptions.directory },
            inputFiles = filesFor(null),
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
