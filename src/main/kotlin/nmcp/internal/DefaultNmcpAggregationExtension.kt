package nmcp.internal

import nmcp.CentralPortalOptions
import nmcp.NmcpAggregationExtension
import nmcp.internal.task.KindAggregation
import nmcp.internal.task.registerNmcpGuessComponentsTask
import nmcp.internal.task.registerPublishToCentralPortalTasks
import org.gradle.api.Action
import org.gradle.api.Project

abstract class DefaultNmcpAggregationExtension(private val project: Project) : NmcpAggregationExtension {

    internal val consumerConfiguration = project.configurations.create(nmcpConsumerConfigurationName) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.configureAttributes(project)
    }

    override fun centralPortal(action: Action<CentralPortalOptions>) {
        val centralPortalOptions = project.objects.newInstance(CentralPortalOptions::class.java)
        action.execute(centralPortalOptions)

        val guessVersion = project.registerNmcpGuessComponentsTask(
            inputFiles = consumerConfiguration
        )
        project.registerPublishToCentralPortalTasks(
            deploymentKind = KindAggregation,
            inputFiles = consumerConfiguration,
            defaultDeploymentName = guessVersion.flatMap { it.outfileFile }.map { it.asFile.readText() },
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
