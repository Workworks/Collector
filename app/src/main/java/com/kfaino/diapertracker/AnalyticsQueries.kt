package com.kfaino.diapertracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📊 资产统计分析与智能查询引擎 (Analytics Queries)
 * 封装闲置变现回血 ROI 统计、安全库存预警与智能采购清单生成。
 * 作为 DataStore 门面下沉的只读分析组件，提供统一的统计模型。
 */
object AnalyticsQueries {

    data class ResaleAnalytics(
        val totalInvested: Double,
        val totalRecovered: Double,
        val netCost: Double,
        val recoveryRate: Double,
        val soldItems: List<Entry>
    )

    fun getResaleAnalytics(allEntries: List<Entry>): ResaleAnalytics {
        val retiredItems = allEntries.filter { it.isRetired }
        val soldItems = retiredItems.filter { it.retiredSoldPrice > 0.0 }
        val totalInvested = retiredItems.sumOf { it.price * it.qty }
        val totalRecovered = retiredItems.sumOf { it.retiredSoldPrice }
        val netCost = (totalInvested - totalRecovered).coerceAtLeast(0.0)
        val recoveryRate = if (totalInvested > 0.0) (totalRecovered / totalInvested) * 100.0 else 0.0

        return ResaleAnalytics(
            totalInvested = totalInvested,
            totalRecovered = totalRecovered,
            netCost = netCost,
            recoveryRate = recoveryRate,
            soldItems = soldItems.sortedByDescending { if (it.retiredAt > 0) it.retiredAt else it.ts }
        )
    }

    fun getLowStockItems(allEntries: List<Entry>): List<Entry> {
        return allEntries.filter { it.isLowStock() }
    }

    fun generateReplenishmentListText(allEntries: List<Entry>): String {
        val lowStock = getLowStockItems(allEntries)
        if (lowStock.isEmpty()) return "🎉 当前所有耗材库存充足，无需补货！"

        val sb = StringBuilder()
        sb.append("🛒【Collecter 智能待采购清单】\n")
        sb.append("----------------------------\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sb.append("生成时间: ${sdf.format(Date())}\n\n")

        for ((idx, item) in lowStock.withIndex()) {
            val diff = (item.minStockThreshold - item.qty).coerceAtLeast(1)
            val lackStr = " (需补 $diff ${item.unit})"
            sb.append("${idx + 1}. 【${item.brand}】\n")
            sb.append("   • 所属分类: ${item.category}\n")
            sb.append("   • 当前库存: ${item.qty} ${item.unit} / 安全预警线: ${item.minStockThreshold} ${item.unit}$lackStr\n")
            if (item.location.isNotBlank()) sb.append("   • 放置位置: ${item.location}\n")
            sb.append("\n")
        }
        sb.append("----------------------------\n")
        sb.append("共计 ${lowStock.size} 项物品急需补货采购")
        return sb.toString()
    }
}
