import java.net.URI

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ggp)
    id("maven-publish")
    id("signing")
}

group = "com.gradleup.nmcp"
version = "0.0.8"

publishing {
    repositories {
        repositories {
            maven {
                name = "OssStaging"
                url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }
    publications.configureEach {
        this as MavenPublication
        if (name == "pluginMaven") {
            artifact(tasks.register("emptySources", Jar::class.java) {
                archiveClassifier = "sources"
            })
            artifact(tasks.register("emptyDocs", Jar::class.java) {
                archiveClassifier = "javadoc"
            })

            groupId = project.rootProject.group.toString()
            version = project.rootProject.version.toString()
            artifactId = project.name
        }

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
                    name.set("MIT License")
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
}

dependencies {
    implementation(libs.json)
    implementation(libs.okio)
    implementation(libs.okhttp)
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