package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface NmcpSettings {
    /**
     * Configures publishing to central portal releases (Maven Central) and central portal snapshots.
     */
    fun centralPortal(action: Action<CentralPortalOptions>)

    /**
     * Applies the `maven-publish` plugin and creates publications for each project in the build.
     *
     * Calling this in a setting plugin will use the same description for every project. For finer
     * control, use a convention plugin and apply `com.gradleup.nmcp` in each published project.
     */
    fun mavenPublish(action: Action<MavenPublishOptions>)
}
