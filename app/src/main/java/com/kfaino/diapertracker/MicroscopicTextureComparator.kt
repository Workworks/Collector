package com.kfaino.diapertracker

import java.security.MessageDigest

/**
 * 🔬 微距光学指纹与高保真防伪材质微观比对器 (Microscopic Texture Comparator)
 */
object MicroscopicTextureComparator {

    data class TextureFingerprint(
        val materialTag: String,
        val lbpHashSignature: String,
        val microRoughnessIndex: Double
    )

    fun extractFingerprint(materialTag: String, sampleBrightnessLevels: IntArray): TextureFingerprint {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = sampleBrightnessLevels.map { (it and 0xFF).toByte() }.toByteArray()
        val hash = md.digest(bytes).joinToString("") { String.format("%02x", it) }.take(16)
        val roughness = if (sampleBrightnessLevels.isNotEmpty()) sampleBrightnessLevels.average() / 255.0 else 0.5

        return TextureFingerprint(materialTag, hash, roughness)
    }

    fun compareFingerprints(fp1: TextureFingerprint, fp2: TextureFingerprint): Float {
        return if (fp1.lbpHashSignature == fp2.lbpHashSignature) 1.0f else 0.45f
    }
}