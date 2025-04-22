package nmcp

import org.gradle.api.provider.Property
import java.time.Duration

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
     * The publication type (optional).
     * One of:
     * - "AUTOMATIC": the deployment is automatically published.
     * - "USER_MANAGED": the deployment is validated but not published. It must be published manually from the Central Portal UI.
     *
     * Default: AUTOMATIC
     */
    abstract val publishingType: Property<String>

    /**
     * A name for the publication (optional).
     *
     * Default: "${project.name}-${project.version}.zip"
     */
    abstract val publicationName: Property<String>

    /**
     * After a deployment has been uploaded, the central portal verifies that it matches the
     * maven central requirements, which may take some time.
     *
     * After waiting, the deployment is either:
     * - VALIDATED: it needs to be manually published in the Central Portal UI.
     * - PUBLISHED: it is published and available on Maven Central.
     * - FAILED: the deployment has failed. You
     *
     * [verificationTimeout] specifies what duration to wait for the verification to complete.
     * You may pass the special value '0' to disable waiting for verification altogether.
     *
     * Default: 10 minutes.
     */
    abstract val verificationTimeout: Property<Duration>

    /**
     * The API endpoint to use (optional).
     *
     * Default: "https://central.sonatype.com/".
     */
    abstract val baseUrl: Property<String>
}