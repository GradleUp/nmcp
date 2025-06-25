package nmcp.internal

import gratatouille.wiring.capitalizeFirstLetter
import nmcp.CentralPortalOptions
import nmcp.internal.task.NmcpPublishWithPublisherApiTask
import nmcp.internal.task.registerNmcpPublishFileByFileTask
import nmcp.internal.task.registerNmcpPublishWithPublisherApiTask
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection

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

    val shortcut = when(name) {
        "aggregation",
        "allPublications" -> name
        else ->  null
    }
    val lifecycleTaskName = shortcut?.let { "publish${it.capitalizeFirstLetter()}ToCentralPortal" }
    val snapshotsLifecycleTaskName = shortcut?.let { "publish${it.capitalizeFirstLetter()}ToCentralPortalSnapshots" }

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

    project.gradle.taskGraph.whenReady {
        if (it.hasTask(task.get())) {
            check(spec.username.isPresent) {
                "Nmcp: username is missing"
            }
            check(spec.password.isPresent) {
                "Nmcp: password is missing"
            }
        }
    }
}


