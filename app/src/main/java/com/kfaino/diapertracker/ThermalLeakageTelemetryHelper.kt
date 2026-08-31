package com.kfaino.diapertracker

/**
 * 🌡️ 多光谱与红外热成像资产热损耗感知助手 (Thermal Leakage Telemetry Helper)
 */
object ThermalLeakageTelemetryHelper {

    data class ThermalScanReport(
        val maxTempCelsius: Double,
        val minTempCelsius: Double,
        val avgTempCelsius: Double,
        val hasAbnormalThermalHotspot: Boolean,
        val analysisAdvice: String
    )

    fun analyzeThermalMatrix(temperatureMatrix: List<Double>): ThermalScanReport {
        if (temperatureMatrix.isEmpty()) {
            return ThermalScanReport(25.0, 25.0, 25.0, false, "无温度数据")
        }

        val maxT = temperatureMatrix.maxOrNull() ?: 25.0
        val minT = temperatureMatrix.minOrNull() ?: 25.0
        val avgT = temperatureMatrix.average()
        val hasHotspot = maxT > 48.0 // 超过48度属于异常发热

        val advice = when {
            hasHotspot -> "⚠️ 检测到局部异常热源 (" + String.format("%.1f", maxT) + "°C)，电器或充电宝可能存在短路自燃风险！"
            maxT - minT > 15.0 -> "💡 局部温差较大，收纳柜可能存在冷热不均或绝热保温失效。"
            else -> "✨ 温度场分布均匀，资产存放环境热学安全。"
        }

        return ThermalScanReport(maxT, minT, avgT, hasHotspot, advice)
    }
}