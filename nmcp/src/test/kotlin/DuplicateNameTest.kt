import java.io.File
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner

class DuplicateNameTest {
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
          .forwardOutput()
          .buildAndFail()

      assert(result.output.contains("duplicate project name"))
  }
}
