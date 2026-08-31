package com.kfaino.diapertracker

/**
 * 🌿 物品碳足迹与全生命周期环境影响精算器 (Carbon Footprint Calculator)
 * 评估每件资产制造、运输与使用周期的碳排放 (kg CO2e)
 */
object CarbonFootprintCalculator {

    data class CarbonReport(
        val totalCarbonKg: Double,
        val greenEcoScore: Int, // 0 ~ 100
        val categoryBreakdown: Map<String, Double>
    )

    fun calculateHouseholdCarbon(store: DataStore): CarbonReport {
        val entries = store.loadAll().filter { it.isIn && !it.isRetired }
        val breakdown = mutableMapOf<String, Double>()
        var totalCarbon = 0.0

        for (e in entries) {
            val factor = when {
                e.category.contains("数码") || e.category.contains("电脑") -> 120.0 // 电子产品碳足迹较高
                e.category.contains("家电") -> 250.0
                e.category.contains("衣服") || e.category.contains("衣橱") -> 15.0
                e.category.contains("书籍") -> 2.5
                else -> 8.0
            }
            val itemCarbon = factor * e.qty
            totalCarbon += itemCarbon
            breakdown[e.category] = (breakdown[e.category] ?: 0.0) + itemCarbon
        }

        val ecoScore = (100 - (totalCarbon / 200).toInt()).coerceIn(40, 95)
        return CarbonReport(totalCarbon, ecoScore, breakdown)
    }
}