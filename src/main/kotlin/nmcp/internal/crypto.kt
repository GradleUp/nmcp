package nmcp.internal

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import okio.BufferedSource
import okio.ByteString.Companion.toByteString
import okio.buffer
import okio.source
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGOutputStream
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRing
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider


internal fun File.signature(key: String, keyPassword: String): String = source().buffer().use { it.signature(key, keyPassword) }

/**
 * Creates a PGP signature from the given BufferedSource
 *
 * Heavily inspired by https://github.com/gradle/gradle/blob/124712713a77a6813e112ae1b68f248deca6a816/subprojects/security/src/main/java/org/gradle/plugins/signing/signatory/pgp/PgpSignatory.java
 */
private fun BufferedSource.signature(key: String, keyPassword: String): String {
    val inputStream = PGPUtil.getDecoderStream(key.byteInputStream())
    val secretKey: PGPSecretKey = JcaPGPSecretKeyRing(inputStream).secretKey
    val decryptor = BcPBESecretKeyDecryptorBuilder(BcPGPDigestCalculatorProvider()).build(keyPassword.toCharArray())
    val privateKey = secretKey.extractPrivateKey(decryptor)

    val generator = PGPSignatureGenerator(BcPGPContentSignerBuilder(secretKey.publicKey.algorithm, PGPUtil.SHA512), secretKey.publicKey)
    generator.init(PGPSignature.BINARY_DOCUMENT, privateKey)

    val scratch = ByteArray(1024)
    var read: Int = read(scratch)
    while (read > 0) {
        generator.update(scratch, 0, read)
        read = read(scratch)
    }

    generator.update(readByteArray())
    val signature = generator.generate()

    return armor {
        signature.encode(it)
    }
}

fun armor(block: (OutputStream) -> Unit): String {
    val os = ByteArrayOutputStream()
    val bufferedOutput = BCPGOutputStream(ArmoredOutputStream(os))

    block(bufferedOutput)
    bufferedOutput.flush()
    bufferedOutput.close()

    return String(os.toByteArray())
}

internal fun File.withExtension(extension: String): File {
    return parentFile.resolve("$name.$extension")
}

internal fun addChecksums(source: File, destination: File) {
    destination.parentFile.mkdirs()
    destination.withExtension("md5").writeText(source.digest("MD5"))
    destination.withExtension("sha1").writeText(source.digest("SHA1"))
    destination.withExtension("sha256").writeText(source.digest("SHA256"))
    destination.withExtension("sha512").writeText(source.digest("SHA512"))
}

internal fun File.digest(name: String): String = source().buffer().use { it.digest(name) }

private fun BufferedSource.digest(name: String): String {
    val md = MessageDigest.getInstance(name)

    val scratch = ByteArray(1024)
    var read: Int = read(scratch)
    while (read > 0) {
        md.update(scratch, 0, read)
        read = read(scratch)
    }

    val digest = md.digest()

    return digest.toByteString().hex()
}
