package com.kfaino.diapertracker

import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 🔒 全家庭 E2EE 端到端零知识加密对撞同步引擎 (E2EE Sync Engine)
 * 基于 AES/GCM/NoPadding 与 SHA-256 口令派生密钥，确保传输链路零知识绝对隐私
 */
object E2eeSyncEngine {

    private const val TAG = "E2eeSyncEngine"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    data class EncryptedEnvelope(
        val ivBase64: String,
        val cipherTextBase64: String,
        val keyFingerprint: String
    )

    private fun deriveKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptPayload(plainJson: String, passphrase: String): EncryptedEnvelope? {
        return try {
            val key = deriveKey(passphrase)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))

            val ivB64 = Base64.getEncoder().encodeToString(iv)
            val dataB64 = Base64.getEncoder().encodeToString(cipherBytes)
            val fp = key.encoded.take(4).joinToString("") { String.format("%02x", it) }

            EncryptedEnvelope(ivB64, dataB64, fp)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "E2EE 加密异常", e)
            null
        }
    }

    fun decryptPayload(envelope: EncryptedEnvelope, passphrase: String): String? {
        return try {
            val key = deriveKey(passphrase)
            val iv = Base64.getDecoder().decode(envelope.ivBase64)
            val cipherBytes = Base64.getDecoder().decode(envelope.cipherTextBase64)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "E2EE 解密异常或口令不匹配", e)
            null
        }
    }
}