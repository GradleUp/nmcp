package nmcp.internal

import nmcp.MavenPublishOptions
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/**
 * Creates missing publications based on the plugins being applied and the tasks found in the project.
 *
 * Some plugins (like the Kotlin multiplatform plugin) create publications automatically, but others do not (like the Kotlin JVM one).
 *
 * [createMissingPublications] makes sure a publication is created for this module and that this publication contains source and javadoc jars.
 *
 * The added javadoc jar is empty and only required to pass the Maven Central checks.
 */
fun Project.createMissingPublications(publications: PublicationContainer)  {
    val emptyJavadoc = emptyJavadoc()
    val javaExtension = extensions.findByType(JavaPluginExtension::class.java)
    if (javaExtension != null) {
        javaExtension.withSourcesJar()
    }

    when {
        project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
            /**
             * Kotlin MPP creates publications but doesn't add javadoc.
             * Note: for Android, the caller needs to opt-in puoblication
             * See https://kotlinlang.org/docs/multiplatform-publish-lib.html#publish-an-android-library
             */
            publications.withType(MavenPublication::class.java).configureEach {
                it.artifact(emptyJavadoc)
            }
        }

        project.plugins.hasPlugin("com.gradle.plugin-publish") -> {
            /**
             * com.gradle.plugin-publish creates all publications
             */
        }

        plugins.hasPlugin("java-gradle-plugin") -> {
            /**
             * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
             */
            publications.withType(MavenPublication::class.java) {
                // Only add sources and javadoc for the main publication
                if (!it.name.endsWith("PluginMarkerMaven")) {
                    it.artifact(emptyJavadoc)
                }
            }
        }

        extensions.findByName("android") != null -> {
            createAndroidPublication("release", emptyJavadoc)
        }

        javaExtension != null -> {
            publications.create("default", MavenPublication::class.java) {
                it.from(components.getByName("java"))
                it.artifact(emptyJavadoc)
            }
        }
    }
}

private fun Project.emptyJavadoc(): TaskProvider<Jar> {
    return tasks.register("nmcpEmptyJavadoc", Jar::class.java) {
        it.archiveClassifier.set("javadoc")
        it.from(
            resources.text.fromString(
                """
                This Javadoc JAR is empty.

                For documentation, see the project website

                """.trimIndent()
            )
        ) {
            it.rename { "readme.txt" }
        }
    }
}

internal fun Project.configurePom(publications: PublicationContainer, options: MavenPublishOptions) {
    publications.configureEach { publication ->
        if (publication !is MavenPublication) {
            return@configureEach
        }
        if (publication.groupId.isNullOrEmpty()) {
            /**
             * Only set the groupId if there is none yet.
             * Gradle plugins change the groupId to use the id of the plugin.
             */
            publication.groupId = options.groupId ?: error("Nmcp: groupId must be set")
        }
        if (publication.artifactId.isNullOrEmpty()) {
            /**
             * Only set the artifactId if there is none yet.
             * KMP changes the artifactId for each publication.
             */
            publication.artifactId = this@configurePom.name
        }

        publication.version = options.version ?: error("Nmcp: version must be set")

        publication.pom {
            it.name.set(publication.name)
            it.description.set(options.description ?: error("Nmcp: description must be set"))
            it.url.set(options.vcsUrl ?: error("Nmcp: vcsUrl must be set"))
            it.scm {
                it.url.set(options.vcsUrl)
                it.connection.set(options.vcsUrl)
                it.developerConnection.set(options.vcsUrl)
            }
            it.licenses {
                it.license {
                    /**
                     * We set the license name to the SPDX identifier
                     * We omit the licenseUrl to play nice with licensee
                     * See https://github.com/cashapp/licensee/issues/374
                     */
                    it.name.set(options.spdxId ?: error("Nmcp: spdxId must be set"))
                }
            }
            it.developers {
                it.developer {
                    it.id.set(options.developer ?: error("Nmcp: developer must be set"))
                    it.name.set(options.developer)
                }
            }
        }
    }
}
