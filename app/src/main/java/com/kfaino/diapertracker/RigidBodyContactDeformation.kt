package com.kfaino.diapertracker

/**
 * 📦 空间物理碰撞干涉与受力形变应力模拟器 (Rigid Body Contact Deformation)
 */
object RigidBodyContactDeformation {

    data class StressAnalysisResult(
        val bottomItemName: String,
        val totalTopWeightKg: Double,
        val maxCompressiveStressKpa: Double,
        val isCrushRisk: Boolean,
        val suggestion: String
    )

    fun evaluateStackPressure(bottomItem: String, stackedItemsWeightKg: Double, contactAreaCm2: Double): StressAnalysisResult {
        val area = contactAreaCm2.coerceAtLeast(10.0)
        // 压强 P = F / A (kPa)
        val forceN = stackedItemsWeightKg * 9.8
        val areaM2 = area / 10000.0
        val stressKpa = (forceN / areaM2) / 1000.0

        val fragile = bottomItem.contains("屏幕") || bottomItem.contains("手办") || bottomItem.contains("帽子")
        val maxAllowedKpa = if (fragile) 2.0 else 15.0
        val crush = stressKpa > maxAllowedKpa

        val sugg = when {
            crush -> "⚠️ 底部【" + bottomItem + "】承受压强 (" + String.format("%.1f", stressKpa) + " kPa) 超标，极易挤压变形破损，请移至顶层！"
            else -> "✨ 堆叠重心稳固，受力在安全承重弹性区间内。"
        }

        return StressAnalysisResult(bottomItem, stackedItemsWeightKg, stressKpa, crush, sugg)
    }
}