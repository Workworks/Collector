package com.kfaino.diapertracker

/**
 * 🌡️ 环境温湿度与光照资产劣化主动预警系统 (Ambient Degradation Monitor)
 * 自动监测茶酒名酿、名画古籍、药箱、相机镜头所处环境湿度，超标主动预警
 */
object AmbientDegradationMonitor {

    data class DegradationAlert(
        val targetCategory: String,
        val riskLevel: RiskLevel,
        val currentTemp: Double,
        val currentHumidity: Double,
        val warningMessage: String
    )

    enum class RiskLevel { SAFE, WARNING, DANGER }

    fun checkRisk(category: String, tempC: Double, humidityPct: Double): DegradationAlert {
        var risk = RiskLevel.SAFE
        var msg = "环境处于安全保存区间。"

        when {
            category.contains("相机") || category.contains("镜头") || category.contains("光学") -> {
                if (humidityPct > 60.0) {
                    risk = RiskLevel.DANGER
                    msg = "⚠️ 湿度达 " + humidityPct + "%，已超过光学镜头安全线(40%~55%)，极易滋生霉菌，请立即开启防潮箱！"
                }
            }
            category.contains("茶") || category.contains("酒") -> {
                if (humidityPct > 75.0 || tempC > 28.0) {
                    risk = RiskLevel.WARNING
                    msg = "⚠️ 茶窖/酒窖温湿度超标，可能影响自然陈化品质。"
                }
            }
            category.contains("药") -> {
                if (tempC > 30.0) {
                    risk = RiskLevel.DANGER
                    msg = "⚠️ 药箱存放温度过高(" + tempC + "°C)，药品成分易变质失效！"
                }
            }
        }

        return DegradationAlert(category, risk, tempC, humidityPct, msg)
    }
}