package com.kfaino.diapertracker

import android.util.Log

/**
 * 🗺️ 空间走道动线热力图与收纳效率评分 (Floor Plan Heatmap & Logistics Score)
 * 分析物品移动流线与出入库频次，计算家庭走道拥堵度与收纳合理性指数
 */
object FloorPlanHeatmapHelper {

    data class HeatPoint(
        val x: Float,
        val y: Float,
        val intensity: Float
    )

    data class StorageEfficiencyReport(
        val efficiencyScore: Int,
        val highFrequencyCount: Int,
        val adviceText: String,
        val heatPoints: List<HeatPoint>
    )

    fun calculateReport(store: DataStore): StorageEfficiencyReport {
        val entries = store.loadAll()
        val inUse = entries.filter { it.isIn && !it.isRetired }

        val heatPoints = mutableListOf<HeatPoint>()
        var highFreq = 0

        for (e in inUse) {
            if (e.pinX >= 0 && e.pinY >= 0) {
                val weight = if (e.isImportant || e.locationHistory.size > 2) 0.9f else 0.4f
                heatPoints.add(HeatPoint(e.pinX, e.pinY, weight))
                if (weight > 0.6f) highFreq++
            }
        }

        val score = (100 - (inUse.size * 2).coerceAtMost(30) + (highFreq * 5).coerceAtMost(20)).coerceIn(60, 98)
        val advice = when {
            score >= 85 -> "🌟 空间动线规划极佳，常用物资均位于走道黄金拾取区！"
            score >= 70 -> "💡 动线良好，建议将高频使用的收纳箱移至离门口更近的置物架。"
            else -> "⚠️ 空间物品分布较分散，建议进行分区归拢。"
        }

        return StorageEfficiencyReport(score, highFreq, advice, heatPoints)
    }
}
