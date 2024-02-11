package nmcp

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.Zip
import java.util.*

class NmcpExtension(private val project: Project) {
    private var mavenPublishFound = false
    private fun register(publicationName: String, spec: NmcpSpec) {
        val publishing = project.extensions.findByType(PublishingExtension::class.java)!!
        val m2Dir = project.layout.buildDirectory.dir("nmcp/m2")
        publishing.apply {
            if (repositories.findByName("nmcp") == null) {
                repositories.apply {
                    maven {
                        it.name = "nmcp"
                        it.url = project.uri(m2Dir)
                    }
                }
            }
        }

        val publication = publishing.publications.findByName(publicationName)
        if (publication == null) {
            val candidates = publishing.publications.map { it.name }
            error("Nmcp: cannot find publication '$publicationName'. Candidates are: '${candidates.joinToString()}'")
        }
        val capitalized =
            publicationName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val publishToNmcpTaskProvider = project.tasks.named("publish${capitalized}PublicationToNmcpRepository")

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
                // Exclude maven-metadata files or the bundle is not recognized
                // See https://slack-chats.kotlinlang.org/t/16407246/anyone-tried-the-https-central-sonatype-org-publish-publish-#c8738fe5-8051-4f64-809f-ca67a645216e
                if (it.name.startsWith("maven-metadata")) {
                    it.exclude()
                }
            }
            it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
            it.archiveFileName.set("publication.zip")
        }

        project.tasks.register("publish${capitalized}PublicationToCentralPortal", NmcpPublishTask::class.java) {
            it.inputFile.set(zipTaskProvider.flatMap { it.archiveFile })
            it.username.set(spec.username)
            it.password.set(spec.password)
            it.publicationType.set(spec.publicationType)
        }
    }

    fun publish(publicationName: String, action: Action<NmcpSpec>) {
        val spec = NmcpSpec(
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
        )
        action.execute(spec)

        project.plugins.withId("maven-publish") {
            mavenPublishFound = true
            register(publicationName, spec)
        }

        project.afterEvaluate {
            if (!mavenPublishFound) {
                error("Nmcp: no 'maven-publish' plugin found")
            }
        }
    }
}