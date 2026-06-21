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
     * By default, Nmcp tries to avoid publishing the checksums.
     * Especially, it filters out the `.sha256` and `.sha512` files, as well as all the signature checksums
     * (`.asc.md5`, `.asc.sha1`, `.asc.sha256`, `.asc.sha512`).
     * This is to play nicer with [Maven Central publishing limits](https://central.sonatype.org/publish/maven-central-publishing-limits/).
     *
     * Default: false
     */
    val publishAllChecksums: Property<Boolean>
}
