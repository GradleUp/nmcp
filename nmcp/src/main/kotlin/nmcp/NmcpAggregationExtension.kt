package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface NmcpAggregationExtension {
    /**
     * Configures publishing to central portal releases (Maven Central) and central portal snapshots.
     *
     * Adds the following tasks:
     * - nmcpPublishAggregationToCentralPortal
     * - nmcpPublishAggregationToCentralPortalSnapshots
     */
    fun centralPortal(action: Action<CentralPortalOptions>)

    /**
     * Applies the `com.gradleup.nmcp` plugin to every project that also applies `maven-publish`.
     *
     * This function is not compatible with breaking project isolation. To be compatible with project isolation,
     * add each subproject to the `nmcpAggregation` configuration dependencies.
     *
     * This was provided as a helper function but indirectly encouraged using non-project isolation compatible practices.
     *
     * Moving forward, use the `com.gradleup.nmcp.settings` or make a convention plugin that applies `com.gradleup.nmcp`
     * to all your projects.
     */
    @Deprecated("Use the settings plugin or a convention plugin instead")
    fun publishAllProjectsProbablyBreakingProjectIsolation()

    /**
     * [allFiles] contains all the files present in the "nmcpAggregation" configuration.
     *
     * This [FileCollection] is a multi-rooted [org.gradle.api.file.FileTree] containing only files.
     *
     * [allFiles] may be used to publish manually to other repositories than Maven Central, such as Google Cloud
     * Storage and or AWS S3.
     */
    val allFiles: FileCollection
}
