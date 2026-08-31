package com.kfaino.diapertracker

import kotlin.math.exp
import kotlin.math.max

/**
 * 📉 全品类二手残值动态衰减曲线与折旧引擎 (Dynamic Depreciation Engine)
 * 内置数码摄影、大家电、汽车周边、奢侈品、工具五金行业权威残值衰减模型
 */
object DynamicDepreciationEngine {

    enum class CategoryDecayCurve(val annualDecayRate: Double, val floorResidualRate: Double) {
        DIGITAL_ELECTRONICS(0.35, 0.10), // 数码电子：首年折旧 35%，残值底线 10%
        LUXURY_JEWELRY(0.08, 0.40),      // 奢侈品珠宝：慢速折旧 8%，残值底线 40%
        HOME_APPLIANCES(0.18, 0.15),     // 家电家具：年折旧 18%，残值底线 15%
        HARDWARE_TOOLS(0.12, 0.25),      // 工具设备：耐用，年折旧 12%，残值底线 25%
        GENERAL_DAILY(0.25, 0.05)        // 普通日用：年折旧 25%，残值底线 5%
    }

    fun getCategoryCurve(categoryName: String): CategoryDecayCurve {
        return when {
            categoryName.contains("数码") || categoryName.contains("手机") || categoryName.contains("电脑") || categoryName.contains("相机") ->
                CategoryDecayCurve.DIGITAL_ELECTRONICS
            categoryName.contains("珠宝") || categoryName.contains("黄金") || categoryName.contains("名表") || categoryName.contains("奢侈品") ->
                CategoryDecayCurve.LUXURY_JEWELRY
            categoryName.contains("家电") || categoryName.contains("家具") ->
                CategoryDecayCurve.HOME_APPLIANCES
            categoryName.contains("工具") || categoryName.contains("五金") ->
                CategoryDecayCurve.HARDWARE_TOOLS
            else ->
                CategoryDecayCurve.GENERAL_DAILY
        }
    }

    /** 计算非线性动态残值与贬值金额 */
    fun calculateCurrentValuation(originalPrice: Double, purchaseDateMs: Long, categoryName: String): Double {
        if (originalPrice <= 0.0) return 0.0
        val now = System.currentTimeMillis()
        val daysOwned = max(0L, (now - purchaseDateMs) / (86400000L))
        val yearsOwned = daysOwned / 365.0

        val curve = getCategoryCurve(categoryName)
        // 指数衰减模型: V(t) = P0 * ( (1 - r)^t * (1 - floor) + floor )
        val residualMultiplier = exp(-curve.annualDecayRate * yearsOwned) * (1.0 - curve.floorResidualRate) + curve.floorResidualRate
        return originalPrice * residualMultiplier.coerceIn(curve.floorResidualRate, 1.0)
    }
}
