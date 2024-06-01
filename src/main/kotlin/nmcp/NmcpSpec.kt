package nmcp

import org.gradle.api.provider.Property

abstract class NmcpSpec {
    abstract val username: Property<String>
    abstract val password: Property<String>
    abstract val publicationType: Property<String>
    abstract val publicationName: Property<String>
    abstract val endpoint: Property<String>
}