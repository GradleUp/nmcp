package nmcp.internal.task

import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GOutputDirectory
import gratatouille.GTask
import nmcp.internal.signature
import nmcp.internal.withExtension

@GTask
fun nmcpGenerateSignatures(
    logger: GLogger,
    inputFiles: GInputFiles,
    signingKey: String?,
    signingKeyPassword: String?,
    outputDirectory: GOutputDirectory,
) {
    outputDirectory.deleteRecursively()
    outputDirectory.mkdirs()

    if (signingKey == null) {
        return
    }

    check(signingKeyPassword != null) {
        "Nmcp: signing key is present but its password is missing"
    }

    inputFiles.filterFiles().forEach {
        val signatureFile = outputDirectory.resolve(it.normalizedPath).withExtension("asc")
        signatureFile.parentFile.mkdirs()
        signatureFile.writeText(it.file.signature(signingKey, signingKeyPassword))
    }
}
