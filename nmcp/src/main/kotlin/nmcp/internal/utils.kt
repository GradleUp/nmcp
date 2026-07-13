package nmcp.internal

import gratatouille.capitalizeFirstLetter
import java.io.File
import nmcp.CentralPortalOptions
import nmcp.internal.task.registerNmcpCheckFilesTask
import nmcp.internal.task.registerNmcpPublishDeploymentTask
import nmcp.internal.task.registerNmcpPublishFileByFileToFileSystemTask
import nmcp.internal.task.registerNmcpPublishFileByFileToSnapshotsTask
import nmcp.internal.task.registerNmcpPublishWithPublisherApiTask
import org.gradle.api.Project
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP
import org.gradle.api.tasks.bundling.Zip

internal val defaultParallelism = 8

internal fun Project.withRequiredPlugin(id: String, block: () -> Unit) {
    var hasPlugin = false

    pluginManager.withPlugin(id) {
        hasPlugin = true
        block()
    }

    afterEvaluate {
        check(hasPlugin) {
            "Nmcp: plugin '$id' must be applied"
        }
    }
}

internal val nmcpConsumerConfigurationName = "nmcpAggregation"
internal val nmcpProducerConfigurationName = "nmcpProducer"
internal val usageValue = "nmcp"

internal fun HasConfigurableAttributes<*>.configureAttributes(project: Project) {
    attributes {
        it.attribute(USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usageValue))
    }
}

internal enum class Kind {
    aggregation,
    allPublications
}

internal fun Project.registerPublishToCentralPortalTasks(
    kind: Kind,
    inputFiles: FileCollection,
    spec: CentralPortalOptions,
    allowEmptyFiles: Provider<Boolean>,
    publishAllChecksums: Provider<Boolean>
): Provider<RegularFile> {
    val name = kind.name

    val checkFilesTaskName = "nmcpCheck${name.capitalizeFirstLetter()}Files"
    val zipTaskName = "nmcpZip${name.capitalizeFirstLetter()}"
    val centralPortalTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortal"
    val centralSnapshotTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortalSnapshots"
    val mavenLocalTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToMavenLocal"

    val centralPortalLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralPortal"
    val snapshotsLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralSnapshots"
    val deprecatedSnapshotsLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralPortalSnapshots"

    val description = when(name) {
        "aggregation" -> "Publishes the aggregation"
        "allPublications" -> "Publishes all the maven publications"
        else ->  null
    }

    val checkFilesTaskProvider = registerNmcpCheckFilesTask(
        allowEmptyFiles = allowEmptyFiles.orElse(false),
        taskName = checkFilesTaskName,
        inputFiles = inputFiles,
    )

    val zipName = "${name}.zip"
    val zipTaskProvider = tasks.register(zipTaskName, Zip::class.java) {
        it.from(inputFiles)
        /**
         * For aggregations, this is a user visible task for the user to inspect the contents before upload
         */
        it.group = if (kind == Kind.aggregation) PUBLISH_TASK_GROUP else null
        it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
        it.archiveFileName.set(zipName)
        it.eachFile {
            val publishAllChecksums = publishAllChecksums.getOrElse(false)
            when {
                it.name.startsWith("maven-metadata") -> {
                    // Exclude maven-metadata files, or the bundle is not recognized
                    // See https://slack-chats.kotlinlang.org/t/16407246/anyone-tried-the-https-central-sonatype-org-publish-publish-#c8738fe5-8051-4f64-809f-ca67a645216e
                    it.exclude()
                }
                !publishAllChecksums && (it.name.endsWith(".sha256")) -> {
                    /**
                     * Stripping `.sha256` checksums leaves out:
                     * - md5 and sha1 checksums:
                     *    - required by Maven Central checks
                     *    - used by Maven for "transport" verification
                     * - sha512:
                     *    - secure way for Gradle to to "security verification"
                     *
                     * see also https://maven.apache.org/resolver/about-checksums.html:
                     *
                     * ```
                     * Hence, the usual argument that "XXX algorithm is unsafe, deprecated, not secure anymore" does not stand in use case of Maven Resolver: there is nothing secure being involved with checksums. Moreover, this is true not only for SHA-1 algorithm, but even for its "elder brother" MD5. Both algorithms are still widely used today as "transport integrity validation" or "error detection" (aka "bit-rot detection").
                     * ```
                     */
                    it.exclude()
                }
                !publishAllChecksums && (it.name.endsWith(".asc.md5") || it.name.endsWith(".asc.sha1") || it.name.endsWith(".asc.sha256") || it.name.endsWith(".asc.sha512")) -> {
                    /**
                     * For signatures, we don't need checksums
                     */
                    it.exclude()
                }
            }
        }
        it.dependsOn(checkFilesTaskProvider)
    }

    val zipProvider = zipTaskProvider.flatMap { it.archiveFile }

    val centralPortalTask = registerNmcpPublishWithPublisherApiTask(
        taskName = centralPortalTaskName,
        taskGroup = PUBLISH_TASK_GROUP,
        taskDescription = "$description to the Central Releases repository.",
        inputFile = zipProvider,
        username = spec.username,
        password = spec.password,
        publicationName = spec.publicationName.orElse(checkFilesTaskProvider.flatMap { it.outputFile }.map { it.asFile.readText() }),
        publishingType = spec.publishingType,
        baseUrl = spec.baseUrl,
        validationTimeoutSeconds = spec.validationTimeout.map { it.seconds },
        publishingTimeoutSeconds = spec.publishingTimeout.map { it.seconds },
    )
    val centralSnapshotsTask = registerNmcpPublishFileByFileToSnapshotsTask(
        taskName = centralSnapshotTaskName,
        taskGroup = PUBLISH_TASK_GROUP,
        taskDescription = "$description to the Central Snapshots repository.",
        username = spec.username,
        password = spec.password,
        snapshotsUrl = project.provider { "https://central.sonatype.com/repository/maven-snapshots/" },
        inputFiles = inputFiles,
        parallelism = spec.uploadSnapshotsParallelism.orElse(defaultParallelism),
    )
    val m2File = File(System.getProperty("user.home")).resolve(".m2/repository")
    registerNmcpPublishFileByFileToFileSystemTask(
        taskName = mavenLocalTaskName,
        taskGroup = PUBLISH_TASK_GROUP,
        taskDescription = "$description to the ~/.m2 Maven Local repository.",
        m2AbsolutePath = project.provider { m2File.absolutePath },
        inputFiles = zipTree(zipProvider),
        parallelism = spec.uploadSnapshotsParallelism.orElse(defaultParallelism),
    )

    if (kind == Kind.aggregation) {
        registerNmcpPublishDeploymentTask(
            taskName = "nmcpPublishDeployment",
            taskGroup = PUBLISH_TASK_GROUP,
            username = spec.username,
            password = spec.password,
            baseUrl = spec.baseUrl,
            deploymentId = project.providers.gradleProperty("nmcpDeploymentId"),
            publishingTimeoutSeconds = spec.publishingTimeout.map { it.seconds },
        )
    }

    /**
     * TODO: those lifecycle tasks probably need to be deprecated and removed.
     * I'm using the `nmcp$Foo` most of the time personally and keeping the number of tasks low is probably better.
     */
    project.tasks.register(centralPortalLifecycleTaskName) {
        it.dependsOn(centralPortalTask)
    }
    project.tasks.register(snapshotsLifecycleTaskName) {
        it.dependsOn(centralSnapshotsTask)
    }
    project.tasks.register(deprecatedSnapshotsLifecycleTaskName) {
        it.dependsOn(snapshotsLifecycleTaskName)
        it.doLast {
            println("'$deprecatedSnapshotsLifecycleTaskName' is deprecated and will be removed in a future release. Use '$centralSnapshotTaskName' instead.")
        }
    }

    /**
     * Detect early if the username and/or password are missing.
     * This gives feedback to the user before compiling all projects.
     */
    project.gradle.taskGraph.whenReady {
        if (it.hasTask(taskPath(project, centralPortalTask.name))) {
            val publishingType = spec.publishingType.orNull
            val validValues = listOf("AUTOMATIC", "USER_MANAGED")
            check(publishingType == null || publishingType in validValues) {
                "Nmcp: invalid Central Portal publishingType value '$publishingType'. Must be one of ${validValues}."
            }
            check(spec.username.isPresent) {
                "Nmcp: Central Portal 'username' is missing, check your Nmcp configuration."
            }
            check(spec.password.isPresent) {
                "Nmcp: Central Portal 'password' is missing, check your Nmcp configuration."
            }
        }
    }

    return zipProvider
}

private fun taskPath(project: Project, taskName: String): String {
    return buildString {
        append(project.path)
        if (project.path != ":") append(":")
        append(taskName)
    }
}
