package nmcp.internal.task

import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("metadata")
internal data class VersionMetadata(
    val modelVersion: String = "1.1.0",
    @XmlElement
    val groupId: String,
    @XmlElement
    val artifactId: String,
    val versioning: Versioning,
    @XmlElement
    val version: String,
) {
    @Serializable
    @XmlSerialName("versioning")
    data class Versioning(
        @XmlElement
        val lastUpdated: String,
        val snapshot: Snapshot,
        @XmlChildrenName("snapshotVersion")
        val snapshotVersions: List<SnapshotVersion>,
    )

    @Serializable
    @XmlSerialName("snapshot")
    data class Snapshot(
        @XmlElement
        val timestamp: String,
        @XmlElement
        val buildNumber: Int,
    )

    @Serializable
    @XmlSerialName("snapshotVersion")
    data class SnapshotVersion(
        @XmlElement
        val classifier: String?,
        @XmlElement
        val extension: String,
        @XmlElement
        val value: String,
        @XmlElement
        val updated: String,
    )
}

@Serializable
@XmlSerialName("metadata")
internal data class ArtifactMetadata(
    @XmlElement
    val groupId: String,
    @XmlElement
    val artifactId: String,
    val versioning: Versioning,
) {
    @Serializable
    @XmlSerialName("versioning")
    data class Versioning(
        @XmlElement
        val latest: String,
        @XmlElement
        val release: String?, // Maybe null if the element is missing or empty if empty
        @XmlElement
        @XmlChildrenName("version")
        val versions: List<String>,
        @XmlElement
        val lastUpdated: String,
    )
}

internal val xml: StringFormat = XML {
    indent = 2
}
