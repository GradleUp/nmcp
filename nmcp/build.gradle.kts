import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.Describables
import com.gradleup.librarian.gradle.Librarian
import org.gradle.internal.DisplayName
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugin.use.internal.DefaultPluginId
import org.gradle.plugin.use.resolve.internal.local.PluginPublication

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ggp)
    alias(libs.plugins.compat)
    alias(libs.plugins.serialization)
}

Librarian.module(project)

gratatouille {
    codeGeneration()
    pluginMarker("com.gradleup.nmcp", "default")
    pluginMarker("com.gradleup.nmcp.aggregation", "default")
}

dependencies {
    implementation(libs.json)
    implementation(libs.okio)
    api(libs.okhttp)
    implementation(libs.xmlutil)

    testImplementation(libs.kotlin.test)
    compileOnly(libs.gradle.min)
}

/**
 * This is so that we can use the plugin if we are an included build
 */
val registry = project.serviceOf<ProjectPublicationRegistry>()

class LocalPluginPublication(private val name: String, private val id: String) : PluginPublication {
    override fun getDisplayName(): DisplayName {
        return Describables.withTypeAndName("plugin", name)
    }

    override fun getPluginId(): PluginId {
        return DefaultPluginId.of(id)
    }
}
registry.registerPublication((project as ProjectInternal).projectIdentity, LocalPluginPublication("nmcp settings plugin", "com.gradleup.nmcp.settings"))
