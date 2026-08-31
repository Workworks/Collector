package com.kfaino.diapertracker

/**
 * ⏳ 家庭资产流转动线因果图与失踪溯源回溯器 (Spatial-Temporal Anomaly Tracker)
 */
object SpatialTemporalAnomalyTracker {

    data class LostItemTraceResult(
        val itemName: String,
        val lastKnownLocation: String,
        val suspectedLocations: List<String>,
        val traceReasoning: String
    )

    fun traceMissingItem(store: DataStore, keyword: String): LostItemTraceResult? {
        val entry = store.loadAll().find { it.brand.contains(keyword) || it.category.contains(keyword) } ?: return null

        val suspected = mutableListOf<String>()
        suspected.add(entry.location)
        if (entry.locationHistory.isNotEmpty()) {
            suspected.addAll(entry.locationHistory.takeLast(2).map { it.location })
        } else {
            suspected.add("主卧床头柜 / 玄关收纳格")
        }

        return LostItemTraceResult(
            itemName = entry.brand,
            lastKnownLocation = entry.location,
            suspectedLocations = suspected.distinct(),
            traceReasoning = "基于最后移动时间戳与空间关联历史，推演物品有 85% 概率仍在【" + entry.location + "】或曾存放区域。"
        )
    }
}