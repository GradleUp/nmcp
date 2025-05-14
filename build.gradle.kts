import java.net.URI

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ggp)
    alias(libs.plugins.nmcp)
    id("maven-publish")
    id("signing")
}

group = "com.gradleup.nmcp"
version = "0.1.4"

publishing {
    publications.create("default", MavenPublication::class.java) {
        from(components.getByName("java"))
        artifact(tasks.register("emptySources", Jar::class.java) {
            archiveClassifier = "sources"
        })
        artifact(tasks.register("emptyDocs", Jar::class.java) {
            archiveClassifier = "javadoc"
        })
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
    implementation(libs.gratatouille.runtime)
    implementation(libs.okhttp.logging.interceptor)
    compileOnly(libs.gradle.min)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

tasks.withType(Sign::class.java).configureEach {
    isEnabled = System.getenv("GPG_KEY").isNullOrBlank().not()
}
