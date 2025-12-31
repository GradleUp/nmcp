package nmcp

import org.gradle.api.provider.Property

abstract class LocalRepositoryOptions {
    /**
     * The name of the repository
     */
    abstract val name: Property<String>

    /**
     * The path. Relative paths are interpreted relative to the project directory.
     */
    abstract val path: Property<String>
}
