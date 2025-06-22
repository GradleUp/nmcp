package nmcp.internal.task

import gratatouille.GInputFiles
import gratatouille.GOutputDirectory
import gratatouille.GTask

@GTask
fun nmcpGeneratePublication(
    groupId: String,
    artifactId: String,
    version: String,
    files: GInputFiles,
    classifiers: List<String>,
    artifactExtensions: List<String>,
    outputDirectory: GOutputDirectory,
) {
    outputDirectory.deleteRecursively()
    outputDirectory.mkdirs()

    // See https://maven.apache.org/repositories/layout.html
    files.forEachIndexed { index, fileWithPath ->
        val path = buildString {
            append("${groupId.replace('.', '/')}/$artifactId/$version/")
            append(artifactId)
            append("-$version")
            val classifier = classifiers.get(index)
            if (classifier.isNotEmpty()) {
                append("-$classifier")
            }
            val extension = artifactExtensions.get(index)
            check(extension.isNotEmpty()) {
                "Nmcp: no extension found for file ${fileWithPath.file}"
            }
            append(".$extension")
        }

        val destination = outputDirectory.resolve(path)
        destination.parentFile.mkdirs()
        fileWithPath.file.copyTo(destination)
    }
}
