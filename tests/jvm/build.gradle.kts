plugins {
    id("org.jetbrains.kotlin.jvm").version("1.9.22").apply(false)
    id("com.gradleup.nmcp").version("0.0.9")
}

val projectGroup = "net.mbonnin.tnmcp"
val projectVersion = "0.0.3"

group = projectGroup
version = projectVersion

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
            artifact(tasks.register("emptySources", Jar::class.java) {
                archiveClassifier = "sources"
            })
            artifact(tasks.register("emptyDocs", Jar::class.java) {
                archiveClassifier = "javadoc"
            })

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
                        name.set("MIT License")
                        url.set("https://github.com/martinbonnin/test-nmcp/blob/master/LICENSE")
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

nmcp {
    publishAllProjectsProbablyBreakingProjectIsolation {
        username = System.getenv("MAVEN_CENTRAL_USERNAME")
        password = System.getenv("MAVEN_CENTRAL_PASSWORD")
        publicationType = "USER_MANAGED"
    }
}

tasks.register("checkZip") {
    inputs.file(tasks.named("zipAggregationPublication").flatMap { (it as Zip).archiveFile })

    doLast {
        val paths = mutableListOf<String>()
        zipTree(inputs.files.singleFile).visit {
            paths.add(this.path)
        }
        check(
            paths.sorted().equals(
                listOf(
                    "net",
                    "net/mbonnin",
                    "net/mbonnin/tnmcp",
                    "net/mbonnin/tnmcp/module1",
                    "net/mbonnin/tnmcp/module1/${project.version}",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-javadoc.jar",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-javadoc.jar.md5",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-javadoc.jar.sha1",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-javadoc.jar.sha256",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-javadoc.jar.sha512",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-sources.jar",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-sources.jar.md5",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-sources.jar.sha1",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-sources.jar.sha256",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}-sources.jar.sha512",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.jar",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.jar.md5",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.jar.sha1",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.jar.sha256",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.jar.sha512",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.module",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.module.md5",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.module.sha1",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.module.sha256",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.module.sha512",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.pom",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.pom.md5",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.pom.sha1",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.pom.sha256",
                    "net/mbonnin/tnmcp/module1/${project.version}/module1-${project.version}.pom.sha512",
                    "net/mbonnin/tnmcp/module2",
                    "net/mbonnin/tnmcp/module2/${project.version}",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-javadoc.jar",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-javadoc.jar.md5",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-javadoc.jar.sha1",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-javadoc.jar.sha256",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-javadoc.jar.sha512",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-sources.jar",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-sources.jar.md5",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-sources.jar.sha1",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-sources.jar.sha256",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}-sources.jar.sha512",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.jar",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.jar.md5",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.jar.sha1",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.jar.sha256",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.jar.sha512",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.module",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.module.md5",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.module.sha1",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.module.sha256",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.module.sha512",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.pom",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.pom.md5",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.pom.sha1",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.pom.sha256",
                    "net/mbonnin/tnmcp/module2/${project.version}/module2-${project.version}.pom.sha512"
                )
            )
        )
    }
}
