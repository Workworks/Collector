package com.kfaino.diapertracker

/**
 * ☀️ 高能辐射与紫外光累积辐照老化预警模型 (UV Radiation Degradation Model)
 */
object UvRadiationDegradationModel {

    data class UvAgingReport(
        val itemName: String,
        val uvIndexCumulativeMj: Double,
        val coatingLifeRemainingPercent: Int,
        val protectionAdvice: String
    )

    fun calculateAging(itemName: String, daysExposedToSun: Int, avgUvIndex: Double): UvAgingReport {
        val totalEnergy = daysExposedToSun * avgUvIndex * 0.15
        val remain = (100 - totalEnergy * 2.0).toInt().coerceIn(0, 100)

        val advice = when {
            remain < 25 -> "⚠️ 紫外线抗老化涂层已严重脆化，防水膜可能渗水，建议喷涂 UV 养护喷雾或移入室内阴凉处！"
            remain < 60 -> "💡 建议避免长期置于南向阳台直晒，适当拉上遮阳帘。"
            else -> "✨ 表面耐候性良好。"
        }

        return UvAgingReport(itemName, totalEnergy, remain, advice)
    }
}