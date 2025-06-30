package nmcp.internal

import gratatouille.wiring.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.internal.task.NmcpPublishWithPublisherApiTask
import nmcp.internal.task.registerNmcpFindDeploymentNameTask
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
import org.gradle.api.tasks.bundling.Zip

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

val nmcpConsumerConfigurationName = "nmcpAggregation"
internal val nmcpProducerConfigurationName = "nmcpProducer"
internal val attribute = "com.gradleup.nmcp"
internal val attributeValue = "bundle"
internal val usageValue = "nmcp"

internal fun HasConfigurableAttributes<*>.configureAttributes(project: Project) {
    attributes {
        it.attribute(
            Attribute.of(attribute, Named::class.java),
            project.objects.named(Named::class.java, attributeValue),
        )
        it.attribute(USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usageValue))
    }
}

internal fun Project.registerPublishToCentralPortalTasks(
    name: String,
    inputFiles: FileCollection,
    action: Action<CentralPortalOptions>,
) {
    val spec = objects.newInstance(CentralPortalOptions::class.java)
    action.execute(spec)

    val releaseTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortal"
    val snapshotTaskName = "nmcpPublish${name.capitalizeFirstLetter()}ToCentralPortalSnapshots"
    val zipTaskName = "nmcpZip${name.capitalizeFirstLetter()}"
    val findDeploymentNameTaskName = "nmcpFind${name.capitalizeFirstLetter()}DeploymentName"

    val shortcut = when(name) {
        "aggregation",
        "allPublications" -> name
        else ->  null
    }
    val lifecycleTaskName = shortcut?.let { "publish${it.capitalizeFirstLetter()}ToCentralPortal" }
    val snapshotsLifecycleTaskName = shortcut?.let { "publish${it.capitalizeFirstLetter()}ToCentralPortalSnapshots" }

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

    if (lifecycleTaskName != null) {
        project.tasks.register(lifecycleTaskName) {
            it.dependsOn(task)
        }
    }

    val snapshots = registerNmcpPublishFileByFileToSnapshotsTask(
        taskName = snapshotTaskName,
        username = spec.username,
        password = spec.password,
        snapshotsUrl = project.provider { "https://central.sonatype.com/repository/maven-snapshots/" },
        inputFiles = inputFiles,
    )
    if (snapshotsLifecycleTaskName != null) {
        project.tasks.register(snapshotsLifecycleTaskName) {
            it.dependsOn(snapshots)
        }
    }

    project.gradle.taskGraph.whenReady {
        if (it.allTasks.any { it is NmcpPublishWithPublisherApiTask }) {
            check(spec.username.isPresent) {
                "Nmcp: username is missing"
            }
            check(spec.password.isPresent) {
                "Nmcp: password is missing"
            }
        }
    }
}


