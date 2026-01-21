package nmcp

import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface NmcpExtension {
    /**
     * Configures publishing all the publications all at once in a single deployment to the Central Portal.
     *
     * - Adds `nmcpPublishAllPublicationsToCentralPortal`
     * - Adds `nmcpPublishAllPublicationsToCentralPortalSnapshots`
     */
    fun publishAllPublicationsToCentralPortal(action: Action<CentralPortalOptions>)

    /**
     * Adds [files] to the `nmcpProducer` outgoing variant.
     * By default, the Nmcp plugin adds all the publications to the `nmcpProducer` outgoing variant.
     * [extraFiles] allows adding more files that are not coming from the traditional `maven-publish` plugin.
     *
     * Use this to add publications whose files are not known until execution time.
     *
     * @param files the files to be added.
     * The given paths are evaluated as per [org.gradle.api.Project.files].
     * Typically, [files] is built from a [org.gradle.api.file.FileTree] or
     * a [org.gradle.api.file.DirectoryProperty] so that the files also contain their relative path.
     */
    fun extraFiles(files: Any)

    /**
     * Applies the `maven-publish` plugin and creates publications.
     */
    fun mavenPublish(action: Action<MavenPublishOptions>)
}
