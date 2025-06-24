package nmcp.internal

import gratatouille.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.internal.task.DeploymentKind
import nmcp.internal.task.KindAggregation
import nmcp.internal.task.KindAll
import nmcp.internal.task.KindSingle
import nmcp.internal.task.registerNmcpPublishFileByFileTask
import nmcp.internal.task.registerNmcpPublishWithPublisherApiTask
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
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

internal val nmcpConsumerConfigurationName = "nmcpAggregation"
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
    deploymentKind: DeploymentKind,
    inputFiles: FileCollection,
    spec: CentralPortalOptions,
) {
    val releaseTaskName: String = when(deploymentKind) {
        KindAggregation -> "nmcpPublishAggregationToCentralPortal"
        KindAll -> "nmcpPublishAllPublicationsToCentralPortal"
        is KindSingle ->  "nmcpPublish${deploymentKind.name.capitalizeFirstLetter()}PublicationToCentralPortal"
    }
    val snapshotTaskName: String = when(deploymentKind) {
        KindAggregation -> "nmcpPublishAggregationToCentralPortalSnapshots"
        KindAll -> "nmcpPublishAllPublicationsToCentralPortalSnapshots"
        is KindSingle ->  "nmcpPublish${deploymentKind.name.capitalizeFirstLetter()}PublicationToCentralPortalSnapshots"
    }

    val lifecycleTaskName: String? = when(deploymentKind) {
        KindAggregation -> "publishAggregationToCentralPortal"
        KindAll -> "publishAllPublicationsToCentralPortal"
        is KindSingle ->  null
    }

    val snapshotsLifecycleTaskName: String? = when(deploymentKind) {
        KindAggregation -> "publishAggregationToCentralPortalSnapshots"
        KindAll -> "publishAllPublicationsToCentralPortalSnapshots"
        is KindSingle ->  null
    }

    val task = registerNmcpPublishWithPublisherApiTask(
        taskName = releaseTaskName,
        inputFiles = inputFiles,
        username = spec.username,
        password = spec.password,
        publicationName = spec.publicationName,
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

    val snapshots = registerNmcpPublishFileByFileTask(
        taskName = snapshotTaskName,
        username = spec.username,
        password = spec.password,
        url = project.provider { "https://central.sonatype.com/repository/maven-snapshots/" },
        inputFiles = inputFiles,
    )
    if (snapshotsLifecycleTaskName != null) {
        project.tasks.register(snapshotsLifecycleTaskName) {
            it.dependsOn(snapshots)
        }
    }
}

