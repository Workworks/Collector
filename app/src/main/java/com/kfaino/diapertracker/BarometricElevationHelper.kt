package com.kfaino.diapertracker

/**
 * ⏱️ 空间环境微气压与海拔动态高度差寻物助手 (Barometric Elevation Helper)
 */
object BarometricElevationHelper {

    data class ElevationLayer(
        val relativeHeightCm: Double,
        val estimatedShelfLayer: Int, // 1~6 层
        val layerDescription: String
    )

    fun calculateShelfLayer(basePressureHpa: Double, currentPressureHpa: Double): ElevationLayer {
        // 国际标准大气压模型：1 hPa 约对应 8.43 米 (843 cm) 高度变化
        val deltaHpa = basePressureHpa - currentPressureHpa
        val heightCm = deltaHpa * 843.0

        val layer = when {
            heightCm < 30.0 -> 1 // 底层
            heightCm < 70.0 -> 2
            heightCm < 110.0 -> 3
            heightCm < 150.0 -> 4
            heightCm < 190.0 -> 5
            else -> 6 // 顶层
        }

        val desc = "货架第 " + layer + " 层 (相对地面约 " + String.format("%.0f", heightCm.coerceAtLeast(0.0)) + " cm)"
        return ElevationLayer(heightCm, layer, desc)
    }
}