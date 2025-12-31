package nmcp.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GInternal
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import java.io.File
import kotlin.io.path.Path
import nmcp.transport.FilesystemTransport
import nmcp.transport.publishFileByFile

@GTask(pure = false)
internal fun nmcpPublishFileByFileToFileSystem(
    logger: GLogger,
    @GInternal
    m2AbsolutePath: String,
    inputFiles: GInputFiles,
    parallelism: Int,
) {
    check(Path(m2AbsolutePath).isAbsolute) {
        "Nmcp: path '$m2AbsolutePath' is not an absolute path"
    }
    logger.info("Nmcp: copying files to $m2AbsolutePath")

    File(m2AbsolutePath).mkdirs()
    publishFileByFile(FilesystemTransport(m2AbsolutePath, logger), inputFiles, parallelism)
}
