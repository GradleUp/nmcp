import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import org.gradle.testkit.runner.GradleRunner

class MainTest {
  @Test
  fun duplicateName() {
    val dst = File("build/testProject")
    val src = File("testProjects/duplicate-name")

    dst.deleteRecursively()
    dst.mkdirs()

    src.copyRecursively(dst, overwrite = true)

      val result = GradleRunner.create()
          .withProjectDir(dst)
          .withArguments("nmcpZipAggregation")
          .buildAndFail()

      assert(result.output.contains("some projects have the same name"))
  }

    @Test
    fun emptyAggregation() {
        val dst = File("build/testProject")
        val src = File("testProjects/empty-aggregation")

        dst.deleteRecursively()
        dst.mkdirs()

        src.copyRecursively(dst, overwrite = true)

        val result = GradleRunner.create()
            .withProjectDir(dst)
            .withArguments("nmcpZipAggregation")
            .buildAndFail()

        assert(result.output.contains("Nmcp: there are no files to publish"))
    }

    @Test
    fun nonLenient() {
        val dst = File("build/testProject")
        val src = File("testProjects/non-lenient")

        dst.deleteRecursively()
        dst.mkdirs()

        src.copyRecursively(dst, overwrite = true)

        val result = GradleRunner.create()
            .withProjectDir(dst)
            .withArguments("nmcpZipAggregation")
            .withDebug(true)
            .buildAndFail()

        assertFalse(result.output.contains("there are no files to publish"))
        assert(result.output.contains("Expected task 'foo' output files to contain exactly one file, however, it contains more than one file"))
    }
}
