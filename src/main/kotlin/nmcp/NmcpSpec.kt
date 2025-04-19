package nmcp

import org.gradle.api.provider.Property

abstract class NmcpSpec {
    /**
     * The central portal username
     */
    abstract val username: Property<String>

    /**
     * The central portal password
     */
    abstract val password: Property<String>

    /**
     * The publication type.
     * One of:
     * - "AUTOMATIC": the deployment is automatically published.
     * - "USER_MANAGED": the deployment is validated but not published. It must be published manually from the Central Portal UI.
     */
    abstract val publicationType: Property<String>

    /**
     * A name for the publication (optional).
     *
     * Default: "${project.name}-${project.version}.zip"
     */
    abstract val publicationName: Property<String>

    /**
     * The API endpoint to use (optional).
     *
     * Default: "https://central.sonatype.com/api/v1/".
     */
    abstract val endpoint: Property<String>

    /**
     * Whether to verify the status of the deployment before returning from the task.
     *
     * Default: true.
     */
    abstract val verifyStatus: Property<Boolean>

    /**
     * Timeout for verification (in seconds).
     *
     * Default: 600 (10 minutes).
     */
    abstract val verificationTimeout: Property<Int>
}