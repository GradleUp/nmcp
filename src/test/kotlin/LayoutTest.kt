import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import nmcp.internal.task.Gav
import nmcp.internal.task.replaceBuildNumber

class LayoutTest {
    @Test
    fun gavAreParsedSuccessfully() {
        assertEquals(Gav("com.example", "foo", "0.0.0"), Gav.from("com/example/foo/0.0.0"))
        assertEquals(Gav("com.example", "bar", "1.0.0-alpha.1"), Gav.from("com/example/bar/1.0.0-alpha.1"))
        assertEquals(Gav("group", "foo", "1.0.0"), Gav.from("group/foo/1.0.0"))
    }

    @Test
    fun invalidGav() {
        assertFails { Gav.from("example") }
        assertFails { Gav.from("com/example") }
        assertFails { Gav.from("com/example/") }
        assertFails { Gav.from("com//1.0.0") }
        assertFails { Gav.from("//") }
    }

    @Test
    fun replaceBuildNumber() {
        val fileName = "module1-0.0.3-20250623.104441-1.jar.asc"
        assertEquals("module1-0.0.3-20250623.104441-42.jar.asc", fileName.replaceBuildNumber("module1", "0.0.3-SNAPSHOT", 42))
    }
}
