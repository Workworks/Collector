package com.kfaino.diapertracker

/**
 * 🥫 极端防灾战备生命维持物资配给算法 (Crisis Survival Ration Allocator)
 */
object CrisisSurvivalRationAllocator {

    data class RationPlan(
        val totalDaysSustainable: Int,
        val dailyKcalPerPerson: Int,
        val dailyWaterLitersPerPerson: Double,
        val emergencyStatusNotice: String
    )

    fun planSurvivalRation(totalFoodKcal: Double, totalCleanWaterLiters: Double, familyMemberCount: Int): RationPlan {
        val count = familyMemberCount.coerceAtLeast(1)
        val dailyKcalNeed = count * 1800.0 // 每人每天维持生命最低 1800 kcal
        val dailyWaterNeed = count * 2.0   // 每人每天 2L 水

        val foodDays = (totalFoodKcal / dailyKcalNeed).toInt()
        val waterDays = (totalCleanWaterLiters / dailyWaterNeed).toInt()
        val sustainDays = minOf(foodDays, waterDays)

        val bottleneck = when {
            waterDays < foodDays -> "饮用水为第一短板"
            foodDays < waterDays -> "卡路里热量为第一短板"
            else -> "水与食物储备均衡"
        }
        val status = "当前物资可支持全家 " + count + " 人维持生存 " + sustainDays + " 天 (" + bottleneck + ")。"

        return RationPlan(sustainDays, 1800, 2.0, status)
    }
}