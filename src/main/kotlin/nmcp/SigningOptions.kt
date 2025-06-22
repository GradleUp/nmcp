package nmcp

import org.gradle.api.provider.Property

abstract class SigningOptions {
    abstract val privateKey: Property<String>

    abstract val privateKeyPassword: Property<String>
}
