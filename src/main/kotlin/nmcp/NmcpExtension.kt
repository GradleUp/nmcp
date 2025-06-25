package nmcp

import org.gradle.api.Action

interface NmcpExtension {
    /**
     * Configures publishing all the publications all at once in a single deployment to the Central Portal.
     *
     * - Adds `nmcpPublishAllPublicationsToCentralPortal`
     * - Adds `nmcpPublishAllPublicationsToCentralPortalSnapshots`
     */
    fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>)
}
