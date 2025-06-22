package nmcp

import org.gradle.api.Action

interface NmcpAggregationExtension {
    /**
     * Configures the central portal parameters
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
     * Configures signing.
     */
    fun sign(action: Action<SigningOptions>)
}
