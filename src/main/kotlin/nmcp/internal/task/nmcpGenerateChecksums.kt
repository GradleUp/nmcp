package nmcp.internal.task

import gratatouille.FileWithPath
import gratatouille.GInputFiles
import gratatouille.GOutputDirectory
import gratatouille.GTask
import kotlin.math.sign
import nmcp.internal.addChecksums
import nmcp.internal.signature
import nmcp.internal.withExtension

@GTask
fun nmcpGenerateChecksums(
    inputFiles: GInputFiles,
    outputDirectory: GOutputDirectory,
) {
    outputDirectory.deleteRecursively()
    outputDirectory.mkdirs()

    inputFiles.filterFiles().forEach {
        addChecksums(it.file, outputDirectory.resolve(it.normalizedPath))
    }
}

internal fun GInputFiles.filterFiles(): List<FileWithPath> {
    return filter { it.file.isFile }
}
