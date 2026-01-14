import java.io.File
import kotlin.test.Test
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
}
