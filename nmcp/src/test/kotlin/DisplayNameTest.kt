import kotlin.test.assertEquals
import nmcp.internal.task.toDisplayName
import org.junit.Test

class DisplayNameTest {
    @Test
    fun prefixTest(){
        assertEquals("com.gradleup.nmcp*", listOf("com.gradleup.nmcp", "com.gradleup.nmcp.aggregation").toDisplayName())
    }
}
