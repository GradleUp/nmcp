import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import nmcp.internal.task.Gav

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
}
