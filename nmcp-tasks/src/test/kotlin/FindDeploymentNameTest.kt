import gratatouille.tasks.FileWithPath
import java.io.File
import kotlin.test.assertEquals
import nmcp.internal.task.nmcpFindDeploymentName
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FindDeploymentNameTest {

    @get:Rule
    internal val tmp = TemporaryFolder()

    private fun assertDeploymentName(inputFiles: List<String>, expected: String) {
        val output = tmp.newFile("output.txt")
        nmcpFindDeploymentName(
            inputFiles = inputFiles.map { FileWithPath(File(it), normalizedPath = it) },
            outputFile = output,
        )
        assertEquals(expected, output.readText())
    }

    @Test
    fun `single artifact`() = assertDeploymentName(
        inputFiles = listOf(
            "com/example/foo/1.0.0/foo-1.0.0.pom",
        ),
        expected = "com.example:foo:1.0.0",
    )

    @Test
    fun `multiple artifacts with different names`() = assertDeploymentName(
        inputFiles = listOf(
            "com/example/foo/1.0.0/foo-1.0.0.pom",
            "com/example/bar/1.0.0/bar-1.0.0.pom",
        ),
        expected = "com.example:*:1.0.0",
    )

    @Test
    fun `multiple artifacts with similar prefixes`() = assertDeploymentName(
        inputFiles = listOf(
            "com/example/foo-bar/1.0.0/foo-bar-1.0.0.pom",
            "com/example/foo-baz/1.0.0/foo-baz-1.0.0.pom",
        ),
        expected = "com.example:foo-ba*:1.0.0",
    )

    @Test
    fun nmcp() = assertDeploymentName(
        inputFiles = listOf(
            "com/gradleup/nmcp/aggregation/com.gradleup.nmcp.aggregation.gradle.plugin/1.0.0/com.gradleup.nmcp.aggregation.gradle.plugin-1.0.0.pom",
            "com/gradleup/nmcp/com.gradleup.nmcp.gradle.plugin/1.0.0/com.gradleup.nmcp.gradle.plugin-1.0.0.pom",
            "com/gradleup/nmcp/kdoc/1.0.0/kdoc-1.0.0.pom",
            "com/gradleup/nmcp/nmcp/1.0.0/nmcp-1.0.0.pom",
        ),
        expected = "com.gradleup.nmcp*:*:1.0.0",
    )

}
