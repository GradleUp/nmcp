import nmcp.NmcpAggregationExtension

plugins {
    id("base")
    alias(libs.plugins.kgp).apply(false)
}

buildscript {
    dependencies {
        classpath("com.gradleup.nmcp:nmcp")
    }
}

apply(plugin = "com.gradleup.nmcp.aggregation")

group = "net.mbonnin.tnmcp"
version = "0.0.3"

subprojects {
    val project = this
    val projectName = project.name

    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val publishing = project.extensions.getByType(PublishingExtension::class.java)

    publishing.publications {
        create("default", MavenPublication::class.java) {
            from(project.components.findByName("java"))
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

            groupId = project.rootProject.group.toString()
            version = project.rootProject.version.toString()
            artifactId = projectName

            pom {
                name.set(projectName)
                packaging = "jar"
                description.set(projectName)
                url.set("https://github.com/martinbonnin/test-nmcp")

                scm {
                    url.set("https://github.com/martinbonnin/test-nmcp")
                    connection.set("https://github.com/martinbonnin/test-nmcp")
                    developerConnection.set("https://github.com/martinbonnin/test-nmcp")
                }

                licenses {
                    license {
                        name.set("MIT")
                    }
                }

                developers {
                    developer {
                        id.set("mbonnin")
                        name.set("mbonnin")
                    }
                }
            }
        }
    }

    project.extensions.getByType(SigningExtension::class.java).apply {
        sign(publishing.publications)
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PRIVATE_KEY_PASSWORD")
        )
    }

    tasks.withType(Sign::class.java) {
        isEnabled = System.getenv("GPG_PRIVATE_KEY").isNullOrBlank().not()
    }
}

extensions.getByType<NmcpAggregationExtension>().apply {
    publishAllProjectsProbablyBreakingProjectIsolation()

    centralPortal {
        username = System.getenv("MAVEN_CENTRAL_USERNAME")
        password = System.getenv("MAVEN_CENTRAL_PASSWORD")
        publishingType = "USER_MANAGED"
    }
}
