package nmcp

import java.time.Duration
import org.gradle.api.provider.Property

abstract class CentralPortalOptions {
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
     * By default, it generates a name from the deployment contents. If the deployment contains several publications, it will
     * show the common parts (typically groupId and version).
     */
    abstract val publicationName: Property<String>

    /**
     * The timeout until the deployment reaches the `VALIDATED` or `FAILED` status.
     *
     * After a deployment has been uploaded, the central portal validates that it matches the
     * maven central requirements, which may take some time.
     *
     * - If [publishingType] is `AUTOMATIC`, the status transitions to `PUBLISHING` automatically after validation.
     * - If [publishingType] is `USER_MANAGED`, the status stays `VALIDATED`. Visit the portal UI
     * to manually trigger the publishing.
     *
     * [validationTimeout] specifies what duration to wait for the verification to complete.
     * You may pass the special value '0' to disable waiting for validation altogether.
     *
     * Default: 10 minutes.
     */
    abstract val validationTimeout: Property<Duration>

    /**
     * The timeout until the deployment reaches the `PUBLISHED` or `FAILED` status.
     *
     * After publishing, the deployment is published and available in Maven Central.
     *
     * Note: it should be very rare that publishing ends in the `FAILED` status. There are
     * no documented instances of this happening. If you see one, please [open an issue](https://github.com/GradleUp/nmcp/issues/new)
     * so we can document this behavior.
     *
     * [publishingTimeout] specifies what duration to wait for the publishing to complete.
     * You may pass the special value '0' to disable waiting for publishing altogether.
     *
     * Default: 0.
     */
    abstract val publishingTimeout: Property<Duration>

    /**
     * The API endpoint to use (optional).
     *
     * Default: "https://central.sonatype.com/".
     */
    abstract val baseUrl: Property<String>

    /**
     * The parallelism level for uploading publications to snapshots.
     * Inside a publication (a similar group/artifact), files are still uploaded serially.
     *
     * Default: 1.
     */
    abstract val uploadSnapshotsParallelism: Property<Int>
}
