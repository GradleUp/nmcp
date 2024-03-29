package nmcp

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized

class NmcpExtension(private val project: Project) {
    private var mavenPublishFound = false
    private val publishAllPublicationsToCentralPortal = project.tasks.register("publishAllPublicationsToCentralPortal")

    private fun register(publicationName: String?, spec: NmcpSpec) {
        if (publicationName != null) {
            registerInternal(publicationName, spec)
        } else {
            val publishing = project.extensions.findByType(PublishingExtension::class.java)!!
            publishing.publications.configureEach {
                registerInternal(it.name, spec)
            }
        }
    }

    private fun registerInternal(publicationName: String, spec: NmcpSpec) {
        val capitalized = publicationName.capitalized()

        val publishing = project.extensions.findByType(PublishingExtension::class.java)!!
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

        val publishToNmcpTaskProvider = project.tasks.named("publish${capitalized}PublicationTo${repoName.capitalized()}Repository")

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
            it.archiveFileName.set("publication$capitalized.zip")
        }


        val publishTaskProvider = project.tasks.register("publish${capitalized}PublicationToCentralPortal", NmcpPublishTask::class.java) {
            it.inputFile.set(zipTaskProvider.flatMap { it.archiveFile })
            it.username.set(spec.username)
            it.password.set(spec.password)
            it.publicationType.set(spec.publicationType)
            it.publicationName.set(spec.publicationName.orElse("${project.name}-${project.version}"))
        }

        publishAllPublicationsToCentralPortal.configure {
            it.dependsOn((publishTaskProvider))
        }

        project.artifacts.add(configurationName, zipTaskProvider)
    }

    private fun publishInternal(publicationName: String?, action: Action<NmcpSpec>) {
        val spec = NmcpSpec(
            project.objects.property(String::class.java),
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

    /**
     * Adds a `publish${PublicationName}PublicationToCentralPortal` task to publish the publication to the central portal.
     *
     * This function requires the `maven-publish` plugin to be applied.
     */
    fun publish(publicationName: String, action: Action<NmcpSpec>) {
        publishInternal(publicationName, action)
    }

    /**
     * Adds a `publishAllPublicationsToCentralPortal` task to publish all the publications to the central portal.
     *
     * Each publication is uploaded as a separate bundle.
     *
     * This function requires the `maven-publish` plugin to be applied.
     *
     */
    fun publishAllPublications(action: Action<NmcpSpec>) {
        publishInternal(null, action)
    }

    /**
     * Adds a `publishAggregatedPublicationToCentralPortal` task to publish a bundle containing all projects.
     *
     * This function requires [publishAllPublications] or [publish] to be called from at least one submodule.
     */
    fun publishAggregation(action: Action<NmcpAggregation>) {
        val configuration = project.configurations.create("nmcpConsumer") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false

            it.configureAttributes(project)
        }

        val aggregation = NmcpAggregation(
            configuration,
            project,
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
        )

        action.execute(aggregation)

        val zipTaskProvider = project.tasks.register("zipAggregationPublication", Zip::class.java) {
            it.from(configuration.elements.map {
                check (it.isNotEmpty()) {
                    "nmcp: no aggregate dependencies found, please apply the 'com.gradleup.nmcp' in your submodules"
                }
                it.map {
                    project.zipTree(it)
                }
            })

            it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
            it.archiveFileName.set("publicationAggregated.zip")
        }

        project.tasks.register("publishAggregatedPublicationToCentralPortal", NmcpPublishTask::class.java) {
            it.inputFile.set(zipTaskProvider.flatMap { it.archiveFile })
            it.username.set(aggregation.username)
            it.password.set(aggregation.password)
            it.publicationType.set(aggregation.publicationType)
            it.publicationName.set(aggregation.publicationName)
        }
    }

    /**
     * Applies the `com.gradleup.nmcp` plugin to every project that also applies `maven-publish` and adds the
     * `publishAggregatedPublicationToCentralPortal` task to publish a bundle containing all projects.
     */
    fun publishAllProjectsProbablyBreakingProjectIsolation(action: Action<NmcpSpec>) {
        check(project === project.rootProject) {
            "publishAllProjectsProbablyBreakingProjectIsolation() must be called from root project"
        }

        val spec = NmcpSpec(
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
            project.objects.property(String::class.java),
        )
        action.execute(spec)

        publishAggregation { aggregation ->
            project.allprojects { aproject ->
                aproject.pluginManager.withPlugin("maven-publish") {
                    aggregation.project(aproject.path)

                    aproject.pluginManager.apply("com.gradleup.nmcp")

                    aproject.extensions.getByType(NmcpExtension::class.java).apply {
                        publishAllPublications(action)
                    }
                }
            }

            aggregation.username.set(spec.username)
            aggregation.password.set(spec.password)
            aggregation.publicationType.set(spec.publicationType)
            aggregation.publicationName.set(spec.publicationName)
        }
    }
}