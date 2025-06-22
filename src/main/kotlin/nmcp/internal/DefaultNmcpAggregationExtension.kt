package nmcp.internal

import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import nmcp.SigningOptions
import nmcp.internal.task.KindAggregation
import nmcp.internal.task.registerNmcpGenerateChecksumsTask
import nmcp.internal.task.registerNmcpGenerateSignaturesTask
import nmcp.internal.task.registerPublishToCentralPortalTasks
import org.gradle.api.Action
import org.gradle.api.Project

abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    private val signingOptions = project.objects.newInstance(SigningOptions::class.java)

    override fun sign(action: Action<SigningOptions>) {
        action.execute(signingOptions)
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
        action.execute(centralPortalOptions)

        val generateSignatures = project.registerNmcpGenerateSignaturesTask(
            taskName = "nmcpGenerateAggregationSignatures",
            inputFiles = consumerConfiguration,
            signingKey = signingOptions.privateKey,
            signingKeyPassword = signingOptions.privateKeyPassword
        )

        val generateChecksums = project.registerNmcpGenerateChecksumsTask(
            taskName = "nmcpGenerateAggregationChecksums",
            inputFiles = project.files(generateSignatures, consumerConfiguration),
        )

        project.registerPublishToCentralPortalTasks(
            deploymentKind = KindAggregation,
            inputFiles = project.files(consumerConfiguration, generateSignatures, generateChecksums),
            defaultDeploymentName = project.provider { "${project.group}:${project.name}:${project.version}" },
            spec = centralPortalOptions
        )
    }

    override fun publishAllProjectsProbablyBreakingProjectIsolation() {
        check(project === project.rootProject) {
            "publishAllProjectsProbablyBreakingProjectIsolation() must be called from root project"
        }

        project.subprojects { aproject ->
            aproject.pluginManager.withPlugin("maven-publish") {
                aproject.pluginManager.apply("com.gradleup.nmcp")

                consumerConfiguration.dependencies.add(aproject.dependencies.create(aproject))
            }
        }
    }
}
