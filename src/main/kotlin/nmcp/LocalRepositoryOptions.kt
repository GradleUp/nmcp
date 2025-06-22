package nmcp

import org.gradle.api.file.Directory
import org.gradle.api.provider.Property

abstract class LocalRepositoryOptions {
    /**
     * The name of the repository. Required if there are multiple repositories
     */
    abstract var name: String?

    /**
     * The directory where to publish.
     */
    abstract var directory: String
}
