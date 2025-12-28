package nmcp.internal

import gratatouille.capitalizeFirstLetter
import java.io.File
import nmcp.CentralPortalOptions
import nmcp.internal.task.registerNmcpFindDeploymentNameTask
import nmcp.internal.task.registerNmcpPublishFileByFileToFileSystemTask
import nmcp.internal.task.registerNmcpPublishFileByFileToSnapshotsTask
import nmcp.internal.task.registerNmcpPublishWithPublisherApiTask
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
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
) {
    val name = kind.name

    val releaseTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortal"
    val snapshotTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortalSnapshots"
    val localTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToMavenLocal"
    val zipTaskName = "nmcpZip${name.capitalizeFirstLetter()}"
    val findDeploymentNameTaskName = "nmcpFind${name.capitalizeFirstLetter()}DeploymentName"

    val description = when(name) {
        "aggregation" -> "Publishes the aggregation"
        "allPublications" -> "Publishes all the maven publications"
        else ->  null
    }
    val centralPortalLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralPortal"
    val deprecatedLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralPortalSnapshots"
    val snapshotsLifecycleTaskName = "publish${name.capitalizeFirstLetter()}ToCentralSnapshots"

    val zipName = "${name}.zip"
    val zipTaskProvider = tasks.register(zipTaskName, Zip::class.java) {
        it.from(inputFiles)
        it.destinationDirectory.set(project.layout.buildDirectory.dir("nmcp/zip"))
        it.archiveFileName.set(zipName)
        it.eachFile {
            // Exclude maven-metadata files, or the bundle is not recognized
            // See https://slack-chats.kotlinlang.org/t/16407246/anyone-tried-the-https-central-sonatype-org-publish-publish-#c8738fe5-8051-4f64-809f-ca67a645216e
            if (it.name.startsWith("maven-metadata")) {
                it.exclude()
            }
        }
    }

    val findDeploymentNameTaskProvider = registerNmcpFindDeploymentNameTask(
        taskName = findDeploymentNameTaskName,
        inputFiles = inputFiles,
    )
    val task = registerNmcpPublishWithPublisherApiTask(
        taskName = releaseTaskName,
        inputFile = zipTaskProvider.flatMap { it.archiveFile },
        username = spec.username,
        password = spec.password,
        publicationName = spec.publicationName.orElse(findDeploymentNameTaskProvider.flatMap { it.outputFile }.map { it.asFile.readText() }),
        publishingType = spec.publishingType,
        baseUrl = spec.baseUrl,
        validationTimeoutSeconds = spec.validationTimeout.map { it.seconds },
        publishingTimeoutSeconds = spec.publishingTimeout.map { it.seconds },
    )
    project.tasks.register(centralPortalLifecycleTaskName) {
        it.group = PUBLISH_TASK_GROUP
        it.description = "$description to the Central Releases repository."
        it.dependsOn(task)
    }

    val centralSnapshots = registerNmcpPublishFileByFileToSnapshotsTask(
        taskName = snapshotTaskName,
        username = spec.username,
        password = spec.password,
        snapshotsUrl = project.provider { "https://central.sonatype.com/repository/maven-snapshots/" },
        inputFiles = inputFiles,
        parallelism = spec.uploadSnapshotsParallelism.orElse(defaultParallelism),
    )
    project.tasks.register(snapshotsLifecycleTaskName) {
        it.group = PUBLISH_TASK_GROUP
        it.description = "$description to the Central Snapshots repository."
        it.dependsOn(centralSnapshots)
    }
    project.tasks.register(deprecatedLifecycleTaskName) {
        it.group = PUBLISH_TASK_GROUP
        it.description = "$description to the Central Snapshots repository."
        it.dependsOn(snapshotsLifecycleTaskName)
        it.doLast {
            println("'$deprecatedLifecycleTaskName' is deprecated and will be removed in a future release. Use '$snapshotsLifecycleTaskName' instead.")
        }
    }

    val m2File = File(System.getProperty("user.home")).resolve(".m2/repository")
    registerNmcpPublishFileByFileToFileSystemTask(
        taskName = localTaskName,
        m2AbsolutePath = project.provider { m2File.absolutePath },
        inputFiles = inputFiles,
        parallelism = spec.uploadSnapshotsParallelism.orElse(defaultParallelism),
    )

    /**
     * Detect early if the username and/or password are missing.
     * This gives feedback to the user before compiling all projects.
     */
    project.gradle.taskGraph.whenReady {
        if (it.hasTask(taskPath(project, task.name))) {
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
}

private fun taskPath(project: Project, taskName: String): String {
    return buildString {
        append(project.path)
        if (project.path != ":") append(":")
        append(taskName)
    }
}
