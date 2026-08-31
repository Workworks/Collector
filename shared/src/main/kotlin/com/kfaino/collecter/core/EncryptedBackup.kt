package com.kfaino.collecter.core

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable authenticated envelope. No password/key persistence and no recovery backdoor. */
object EncryptedBackup {
    private val magic="COLLECTER-ENC-1\n".toByteArray(Charsets.US_ASCII)
    private const val ITERATIONS=600000
    val MAX_BYTES=BackupDocument.MAX_BYTES + 128
    private fun key(password: CharArray, salt: ByteArray): ByteArray {
        require(password.size in 12..1024) { "备份密码需 12–1024 个字符" }
        val spec=PBEKeySpec(password,salt,ITERATIONS,256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    fun encrypt(json: String, password: CharArray): ByteArray {
        BackupDocument.parse(json)
        val salt=ByteArray(16); val nonce=ByteArray(12)
        SecureRandom().apply { nextBytes(salt); nextBytes(nonce) }
        val header=magic+salt+nonce
        val derived=key(password,salt)
        return try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(derived,"AES"),GCMParameterSpec(128,nonce))
            cipher.updateAAD(header)
            header+cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        } finally { derived.fill(0) }
    }
    fun decrypt(bytes: ByteArray, password: CharArray): String {
        require(bytes.size in (magic.size+16+12+16)..MAX_BYTES) { "加密备份长度无效" }
        val input=ByteBuffer.wrap(bytes)
        val found=ByteArray(magic.size).also(input::get)
        require(found.contentEquals(magic)) { "不是受支持的 Collecter 加密备份" }
        val salt=ByteArray(16).also(input::get); val nonce=ByteArray(12).also(input::get)
        val header=bytes.copyOfRange(0,input.position())
        val encrypted=ByteArray(input.remaining()).also(input::get)
        val derived=key(password,salt)
        return try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(derived,"AES"),GCMParameterSpec(128,nonce))
            cipher.updateAAD(header)
            val plain=cipher.doFinal(encrypted)
            try { String(plain,Charsets.UTF_8).also { BackupDocument.parse(it) } } finally { plain.fill(0) }
        } finally { derived.fill(0) }
    }
}
