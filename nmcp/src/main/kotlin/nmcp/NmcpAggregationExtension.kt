package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property

interface NmcpAggregationExtension {
    /**
     * Configures publishing to central portal releases (Maven Central) and central portal snapshots.
     */
    fun centralPortal(action: Action<CentralPortalOptions>)

    /**
     * Registers an extra task to publish the aggregation to.
     */
    fun localRepository(action: Action<LocalRepositoryOptions>)

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
    @Deprecated("publishAllProjectsProbablyBreakingProjectIsolation() breaks project isolation. This API is convenient" +
        "and there are no plans to remove it but it is marked as deprecated as a signal that it's not" +
        "compatible with the latest Gradle features.\n" +
        "If you want to enable isolated projects, you should look at settings plugin or a convention plugin instead.")
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

    /**
     * By default, Nmcp errors if the aggregation is empty.
     * Set this to true to allow empty aggregations.
     */
    val allowEmptyAggregation: Property<Boolean>

    /**
     * By default, Nmcp errors if there are duplicate project names because
     * it confuses the dependency resolution algorithm.
     *
     * If you have duplicate project names that do not contribute publishing,
     * set this to true to allow them.
     *
     * See https://github.com/gradle/gradle/issues/36167 for more details.
     */
    val allowDuplicateProjectNames: Property<Boolean>
}
