package com.kfaino.diapertracker

/**
 * 🪵 实木家具、皮具与古籍纸张等温吸附滞后回线精算引擎 (Wood & Leather Hysteresis Engine)
 */
object WoodLeatherHysteresisEngine {

    data class MoistureEquilibriumReport(
        val materialName: String,
        val equilibriumMoisturePercent: Double,
        val isCrackingRisk: Boolean, // 开裂风险（过干）
        val isMoldRisk: Boolean,     // 发霉风险（过湿）
        val optimalActionText: String
    )

    fun evaluateMoisture(materialName: String, relativeHumidity: Double, temperatureC: Double): MoistureEquilibriumReport {
        // Hailwood-Horrobin 等温吸附简化模型
        val emc = (0.01 * relativeHumidity * (1.0 + 0.005 * (30.0 - temperatureC)) * 14.5).coerceIn(4.0, 25.0)
        val crack = emc < 7.0
        val mold = emc > 16.0

        val action = when {
            crack -> "⚠️ 环境极度干燥，实木/真皮易收缩开裂，建议加湿或涂抹保养蜂蜡/滋润油。"
            mold -> "⚠️ 含水率偏高，极易滋生霉斑与书虫，请放置防潮卡与吸湿盒。"
            else -> "✨ 处于黄金平衡含水率区间 (" + String.format("%.1f", emc) + "%)。"
        }

        return MoistureEquilibriumReport(materialName, emc, crack, mold, action)
    }
}