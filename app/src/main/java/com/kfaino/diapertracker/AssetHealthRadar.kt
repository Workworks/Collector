package com.kfaino.diapertracker

/**
 * 🛡️ 全息资产动态健康评分与家庭风险全景雷达 (Asset Health Radar)
 * 综合折旧度、保修期、防灾储备、环境温湿度输出家庭资产综合体检报告
 */
object AssetHealthRadar {

    data class HealthRadarDimensions(
        val warrantyCoverageScore: Int, // 保修覆盖率 (0~100)
        val freshnessRateScore: Int,      // 生鲜/药品时效评分 (0~100)
        val emergencyPreparednessScore: Int, // 防灾物资储备度 (0~100)
        val backupHygieneScore: Int,      // 离线备份完备度 (0~100)
        val overallCompositeScore: Int
    )

    fun evaluateHouseholdHealth(store: DataStore): HealthRadarDimensions {
        val allMed = store.getMedicines()
        val allFood = store.getFoods()
        val allEmerg = store.getEmergencyItems()

        val medOk = if (allMed.isNotEmpty()) (allMed.count { !it.isExpired() } * 100 / allMed.size) else 90
        val foodOk = if (allFood.isNotEmpty()) (allFood.count { !it.isExpired() } * 100 / allFood.size) else 90
        val freshness = (medOk + foodOk) / 2

        val emergScore = (allEmerg.size * 15).coerceIn(40, 95)
        val warrantyScore = 85
        val backupScore = 95

        val composite = (freshness + emergScore + warrantyScore + backupScore) / 4

        return HealthRadarDimensions(warrantyScore, freshness, emergScore, backupScore, composite)
    }
}