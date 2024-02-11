import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.9.22")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
}

gradlePlugin {
    plugins {
        create("nmcp") {
            id = "com.gradleup.nmcp"
            implementationClass = "nmcp.NmcpPlugin"
            description = pluginDescription
        }
    }
}

group = "com.gradleup.nmcp"
version = "0.0.1"
val pluginDescription = "Plugin that helps you publish to the Central Portal (https://central.sonatype.org/)"

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
    publications.all {
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
            packaging = "jar"
            description.set(pluginDescription)
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


dependencies {
    implementation("com.squareup.okio:okio:3.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}

fun isTag(): Boolean {
    val ref = System.getenv("GITHUB_REF")

    return ref?.startsWith("refs/tags/") == true
}

tasks.register("ci")

if (isTag()) {
    rootProject.tasks.named("ci") {
        dependsOn(tasks.named("publishAllPublicationsToOssStagingRepository"))
    }
}


tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

tasks.withType(Sign::class.java).configureEach {
    isEnabled = System.getenv("GPG_KEY").isNullOrBlank().not()
}