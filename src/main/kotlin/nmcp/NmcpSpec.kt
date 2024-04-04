package nmcp

import org.gradle.api.provider.Property

class NmcpSpec(
    val username: Property<String>,
    val password: Property<String>,
    val publicationType: Property<String>,
    val publicationName: Property<String>,
    val endpoint: Property<String>,
)