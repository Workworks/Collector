package com.kfaino.diapertracker

import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Detached SHA256withRSA verification; absent or malformed material always fails closed. */
object DexSignatureVerifier {
    fun verify(dex: File, signature: File, publicKeyBase64: String): Boolean {
        if (publicKeyBase64.isBlank() || !dex.isFile || !signature.isFile || signature.length() > 16384) return false
        return try {
            val decoder = Base64.getDecoder()
            val key = KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(decoder.decode(publicKeyBase64.filterNot(Char::isWhitespace))))
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(key)
            dex.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    verifier.update(buffer, 0, count)
                }
            }
            verifier.verify(decoder.decode(signature.readText(Charsets.UTF_8).filterNot(Char::isWhitespace)))
        } catch (e: Exception) {
            System.err.println("DexSignatureVerifier: rejected invalid signature material (${e.javaClass.simpleName})")
            false
        }
    }
}
