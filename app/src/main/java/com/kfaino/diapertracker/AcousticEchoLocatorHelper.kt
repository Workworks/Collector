package com.kfaino.diapertracker

import kotlin.math.abs

/**
 * 🔊 空间声学指纹与敲击回声室内定位助手 (Acoustic Echo Locator Helper)
 * 纯离线脉冲回声延迟分析与收纳柜材质（金属/木质/塑料）声学响应推断
 */
object AcousticEchoLocatorHelper {

    data class AcousticSignature(
        val estimatedRoomVolumeM3: Double,
        val detectedMaterial: String,
        val echoReverberationMs: Long,
        val confidence: Float
    )

    fun analyzeEcho(echoDelayMs: Long, peakFrequencyHz: Double): AcousticSignature {
        val vol = (echoDelayMs * 0.343).coerceIn(5.0, 120.0) // 基于声速估算空间尺度
        val mat = when {
            peakFrequencyHz > 3000.0 -> "金属/铁艺收纳柜"
            peakFrequencyHz in 1000.0..3000.0 -> "木质衣柜/实木书架"
            else -> "塑料/亚克力收纳盒"
        }

        return AcousticSignature(
            estimatedRoomVolumeM3 = vol,
            detectedMaterial = mat,
            echoReverberationMs = echoDelayMs,
            confidence = 0.86f
        )
    }
}