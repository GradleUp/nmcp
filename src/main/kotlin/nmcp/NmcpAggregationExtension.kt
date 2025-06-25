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
     */
    fun publishAllProjectsProbablyBreakingProjectIsolation()

    /**
     * [allFiles] contains all the files present in the "nmcpAggregation" configuration
     */
    val allFiles: FileCollection
}
