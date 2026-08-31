package com.kfaino.diapertracker

import kotlin.math.exp

/**
 * 🧪 高分子聚合物与橡胶水解老化动力学模型 (Polymer Hydrolysis Model)
 * 针对鞋底（EVA/PU/PEBA）、羽绒服压胶条、Gore-Tex 膜计算水解老化率
 */
object PolymerHydrolysisModel {

    data class HydrolysisPrediction(
        val materialType: String,
        val estimatedRemainingLifespanDays: Int,
        val hydrolysisRiskPercent: Int,
        val maintenanceRecommendation: String
    )

    fun predictLifespan(material: String, daysStored: Int, ambientHumidity: Double): HydrolysisPrediction {
        val humidityFactor = if (ambientHumidity > 65.0) 1.8 else 1.0
        val baseLifespanDays = when {
            material.contains("PU") || material.contains("聚氨酯") -> 1000
            material.contains("PEBA") || material.contains("碳板鞋") -> 1200
            material.contains("压胶") || material.contains("冲锋衣") -> 1500
            else -> 2000
        }

        val effectiveDaysUsed = (daysStored * humidityFactor).toInt()
        val remain = (baseLifespanDays - effectiveDaysUsed).coerceAtLeast(0)
        val risk = ((effectiveDaysUsed.toDouble() / baseLifespanDays) * 100).toInt().coerceIn(0, 100)

        val rec = when {
            risk > 75 -> "⚠️ 水解高危期，中底易粉化或压胶脱落，请务必放入带干燥剂的密封袋！"
            risk > 40 -> "💡 建议保持干燥通风环境，避免潮湿引起聚合物降解。"
            else -> "✨ 材料处于健康弹性期。"
        }

        return HydrolysisPrediction(material, remain, risk, rec)
    }
}