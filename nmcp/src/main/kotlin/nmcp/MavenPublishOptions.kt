package nmcp

/**
 * The mandatory information for publishing a Maven artifact.
 *
 * Nmcp purposedly doesn't allow customizing all the pom fields. If you need
 * to customize the pom file, create the publications outside nmcp.
 */
class MavenPublishOptions {
    var groupId: String? = null
    var version: String? = null
    var description: String? = null
    var vcsUrl: String? = null
    var developer: String? = null
    var spdxId: String? = null
}
