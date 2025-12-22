package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface NmcpSettings {
    /**
     * Configures publishing to central portal releases (Maven Central) and central portal snapshots.
     *
     * Adds the following tasks:
     * - nmcpPublishAggregationToCentralPortal
     * - nmcpPublishAggregationToCentralPortalSnapshots
     */
    fun centralPortal(action: Action<CentralPortalOptions>)
}
