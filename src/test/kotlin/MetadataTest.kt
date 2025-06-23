import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import nmcp.internal.ArtifactMetadata
import nmcp.internal.VersionMetadata
import nmcp.internal.task.encodeToXml
import nmcp.internal.xml

class MetadataTest {
    @Test
    fun artifactMetadataIsDecodedSuccessfully() {
        // language=xml
        val xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.apollographql.apollo</groupId>
              <artifactId>apollo-api-jvm</artifactId>
              <versioning>
                <latest>5.0.0-SNAPSHOT</latest>
                <release/>
                <versions>
                  <version>4.1.2-SNAPSHOT</version>
                  <version>5.0.0-SNAPSHOT</version>
                </versions>
                <lastUpdated>20250618175334</lastUpdated>
              </versioning>
            </metadata>
        """.trimIndent()

        xml.decodeFromString<ArtifactMetadata>(xmlData).apply {
            assertEquals("com.apollographql.apollo", groupId)
            assertEquals("apollo-api-jvm", artifactId)
            versioning.apply {
                assertEquals("5.0.0-SNAPSHOT", latest)
                assertEquals("", release)
                assertEquals(
                    listOf(
                        "4.1.2-SNAPSHOT",
                        "5.0.0-SNAPSHOT",
                    ),
                    versions,
                )
                assertEquals("20250618175334", lastUpdated)
            }
        }
    }

    @Test
    fun artifactMetadataIsEncodedSuccessfully() {
        /**
         * Note that <release></release> should probably be <release />
         * See https://github.com/pdvrieze/xmlutil/issues/290
         */
        // language=xml
        val xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.apollographql.apollo</groupId>
              <artifactId>apollo-api-jvm</artifactId>
              <versioning>
                <latest>5.0.0-SNAPSHOT</latest>
                <release></release>
                <versions>
                  <version>4.1.2-SNAPSHOT</version>
                  <version>5.0.0-SNAPSHOT</version>
                </versions>
                <lastUpdated>20250618175334</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()
        val metadata = ArtifactMetadata(
            groupId = "com.apollographql.apollo",
            artifactId = "apollo-api-jvm",
            versioning = ArtifactMetadata.Versioning(
                latest = "5.0.0-SNAPSHOT",
                release = "",
                versions = listOf(
                    "4.1.2-SNAPSHOT",
                    "5.0.0-SNAPSHOT",
                ),
                lastUpdated = "20250618175334",
            ),
        )

        val result = encodeToXml(metadata)

        assertEquals(xmlData, result,)
    }

    @Test
    fun versionMetadataIsDecodedSuccessfully() {
        // language=xml
        val metadata = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.apollographql.apollo</groupId>
              <artifactId>apollo-api-jvm</artifactId>
              <versioning>
                <lastUpdated>20250618175232</lastUpdated>
                <snapshot>
                  <timestamp>20250618.175232</timestamp>
                  <buildNumber>62</buildNumber>
                </snapshot>
                <snapshotVersions>
                  <snapshotVersion>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <extension>module</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <extension>pom</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>javadoc</classifier>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>sources</classifier>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
              <version>5.0.0-SNAPSHOT</version>
            </metadata>
        """.trimIndent()

        xml.decodeFromString<VersionMetadata>(metadata).apply {
            assertEquals("com.apollographql.apollo", groupId)
            assertEquals("apollo-api-jvm", artifactId)
            versioning.apply {
                assertEquals("20250618175232", lastUpdated)
                snapshot.apply {
                    assertEquals("20250618.175232", this.timestamp)
                    assertEquals(62, this.buildNumber)
                }
                assertEquals(
                    listOf(
                        VersionMetadata.SnapshotVersion(null, extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                        VersionMetadata.SnapshotVersion(null, extension = "module", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                        VersionMetadata.SnapshotVersion(null, extension = "pom", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                        VersionMetadata.SnapshotVersion(classifier = "javadoc", extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                        VersionMetadata.SnapshotVersion(classifier = "sources", extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                    ),
                    snapshotVersions,
                )
                assertEquals("5.0.0-SNAPSHOT", version)
            }
        }
    }

    @Test
    fun versionMetadataIsEncodedSuccessfully() {
        // language=xml
        val xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.apollographql.apollo</groupId>
              <artifactId>apollo-api-jvm</artifactId>
              <versioning>
                <lastUpdated>20250618175334</lastUpdated>
                <snapshot>
                  <timestamp>20250618.175232</timestamp>
                  <buildNumber>62</buildNumber>
                </snapshot>
                <snapshotVersions>
                  <snapshotVersion>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <extension>module</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <extension>pom</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>javadoc</classifier>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>sources</classifier>
                    <extension>jar</extension>
                    <value>5.0.0-20250618.175232-62</value>
                    <updated>20250618175232</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
              <version>5.0.0-SNAPSHOT</version>
            </metadata>
        """.trimIndent()

        val metadata = VersionMetadata(
            groupId = "com.apollographql.apollo",
            artifactId = "apollo-api-jvm",
            versioning = VersionMetadata.Versioning(
                lastUpdated = "20250618175334",
                snapshot = VersionMetadata.Snapshot(
                    timestamp = "20250618.175232",
                    buildNumber = 62
                ),
                snapshotVersions = listOf(
                    VersionMetadata.SnapshotVersion(null, extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                    VersionMetadata.SnapshotVersion(null, extension = "module", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                    VersionMetadata.SnapshotVersion(null, extension = "pom", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                    VersionMetadata.SnapshotVersion(classifier = "javadoc", extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                    VersionMetadata.SnapshotVersion(classifier = "sources", extension = "jar", value = "5.0.0-20250618.175232-62", updated = "20250618175232"),
                ),
            ),
            modelVersion = "1.1.0",
            version = "5.0.0-SNAPSHOT"
        )

        val result = encodeToXml(metadata)

        assertEquals(xmlData, result,)
    }

    @Test
    fun releaseMightByMissing() {
        // language=xml
        val xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>net.mbonnin.tnmcp</groupId>
              <artifactId>module1</artifactId>
              <versioning>
                <latest>0.0.5-SNAPSHOT</latest>
                <lastUpdated>20250514205236</lastUpdated>
              </versioning>
            </metadata>
        """.trimIndent()

        val data = xml.decodeFromString<ArtifactMetadata>(xmlData)
        assertNull(data.versioning.release)
    }
}
