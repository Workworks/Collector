package com.kfaino.diapertracker

import kotlin.math.sqrt

/**
 * 🧲 空间环境地磁异常指纹寻物辅助助手 (Magnetic Anomaly Locator Helper)
 */
object MagneticAnomalyLocatorHelper {

    data class MagneticPoint(
        val roomId: String,
        val bX: Float,
        val bY: Float,
        val bZ: Float
    ) {
        val totalMagnitude: Float get() = sqrt(bX * bX + bY * bY + bZ * bZ)
    }

    fun matchRoom(reading: FloatArray, knownPoints: List<MagneticPoint>): String {
        if (reading.size < 3 || knownPoints.isEmpty()) return "未知空间"
        val currentMag = sqrt(reading[0] * reading[0] + reading[1] * reading[1] + reading[2] * reading[2])

        var bestMatch = knownPoints.first().roomId
        var minDiff = Float.MAX_VALUE

        for (pt in knownPoints) {
            val diff = Math.abs(pt.totalMagnitude - currentMag)
            if (diff < minDiff) {
                minDiff = diff
                bestMatch = pt.roomId
            }
        }
        return if (minDiff < 8.0f) bestMatch else "空间磁场指纹采集中"
    }
}