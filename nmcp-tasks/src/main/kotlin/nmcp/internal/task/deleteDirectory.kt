package nmcp.internal.task

import gratatouille.tasks.GInternal
import gratatouille.tasks.GTask
import java.io.File

@GTask(pure = false)
internal fun cleanupDirectory(
    @GInternal
    directory: String
) {
    File(directory).deleteRecursively()
}
