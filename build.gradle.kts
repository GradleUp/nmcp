import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugin.use.internal.DefaultPluginId
import org.gradle.plugin.use.resolve.internal.local.PluginPublication

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ggp)
    alias(libs.plugins.nmcp)
    alias(libs.plugins.compat)
    alias(libs.plugins.serialization)
    id("maven-publish")
    id("signing")
}

group = "com.gradleup.nmcp"
version = "0.2.0"

compatPatrouille {
    java(17)
    kotlin("1.9.0")
}

publishing {
    publications.create("default", MavenPublication::class.java) {
        from(components.getByName("java"))
        artifact(
            tasks.register("emptySources", Jar::class.java) {
                archiveClassifier = "sources"
            },
        )
        artifact(
            tasks.register("emptyDocs", Jar::class.java) {
                archiveClassifier = "javadoc"
            },
        )
    }

    publications.configureEach {
        this as MavenPublication
        pom {
            name.set(project.name)
            description.set("NMCP")
            url.set("https://github.com/gradleup/nmcp")

            scm {
                url.set("https://github.com/gradleup/nmcp")
                connection.set("https://github.com/gradleup/nmcp")
                developerConnection.set("https://github.com/gradleup/nmcp")
            }

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://github.com/gradleup/nmcp/blob/master/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("GradleUp developers")
                    name.set("GradleUp developers")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)

    useInMemoryPgpKeys(System.getenv("GPG_KEY"), System.getenv("GPG_KEY_PASSWORD"))
}

gratatouille {
    codeGeneration()
    pluginMarker("com.gradleup.nmcp", "default")
    pluginMarker("com.gradleup.nmcp.aggregation", "default")
}

nmcp {
    centralPortal {
        username = System.getenv("CENTRAL_PORTAL_USERNAME")
        password = System.getenv("CENTRAL_PORTAL_PASSWORD")
        publishingType.set("USER_MANAGED")
    }
}

dependencies {
    implementation(libs.json)
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.xmlutil)

    testImplementation(libs.kotlin.test)
    compileOnly(libs.gradle.min)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

tasks.withType(Sign::class.java).configureEach {
    isEnabled = System.getenv("GPG_KEY").isNullOrBlank().not()
}


/**
 * This is so that we can use the plugin if we are an included build
 */
val registry = project.serviceOf<ProjectPublicationRegistry>()

class LocalPluginPublication(private val name: String, private val id: String) : PluginPublication {
    override fun getDisplayName(): DisplayName {
        return Describables.withTypeAndName("plugin", name)
    }

    override fun getPluginId(): PluginId {
        return DefaultPluginId.of(id)
    }
}
registry.registerPublication((project as ProjectInternal).projectIdentity, LocalPluginPublication("nmcp settings plugin", "com.gradleup.nmcp.settings"))
