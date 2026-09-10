package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property

interface NmcpExtension {
    /**
     * Configures publishing all the publications all at once in a single deployment to the Central Portal.
     *
     * - Adds `nmcpPublishAllPublicationsToCentralPortal`
     * - Adds `nmcpPublishAllPublicationsToCentralPortalSnapshots`
     */
    @Deprecated("This duplicates the com.gradleup.nmcp.aggregation functionality. Either apply com.gradleup.nmcp.aggregation or the settings plugin to publish an aggregation.")
    fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>)

    /**
     * Adds [files] to the `nmcpProducer` outgoing variant.
     * By default, the Nmcp plugin adds all the publications to the `nmcpProducer` outgoing variant.
     * [extraFiles] allows adding more files that are not coming from the traditional `maven-publish` plugin.
     *
     * Use this to add publications whose files are not known until execution time.
     *
     * @param artifactNotation the files to be added.
     * The given paths are evaluated as per [org.gradle.api.artifacts.dsl.ArtifactHandler.add].
     * Typically, [artifactNotation] is a directory or a task producing a directory.
     */
    fun extraFiles(artifactNotation: Any)
}
