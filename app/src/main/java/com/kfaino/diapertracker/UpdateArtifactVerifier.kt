package com.kfaino.diapertracker

import java.io.File
import java.security.MessageDigest

object UpdateArtifactVerifier {
    fun verify(file: File, expectedSize: Long, expectedSha256: String): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        if (expectedSize > 0L && file.length() != expectedSize) return false
        if (expectedSha256.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256.removePrefix("sha256:").trim(), ignoreCase = true)
    }
}
