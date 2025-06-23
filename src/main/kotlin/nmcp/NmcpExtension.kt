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

    /**
     * Configures publishing a single publication to the Central Portal.
     *
     * - Adds `nmcpPublish${publicationName.capitalized()}PublicationToCentralPortal`
     */
    fun publishToCentralPortal(publicationName: String, action: Action<CentralPortalOptions>)
}
