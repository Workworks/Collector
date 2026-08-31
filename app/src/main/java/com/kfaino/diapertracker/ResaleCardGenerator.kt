package com.kfaino.diapertracker

/**
 * 🏷️ 闲鱼/转转标准化二手商品长图卡片生成排版器 (Resale Card Generator)
 */
object ResaleCardGenerator {

    data class ResaleCardLayout(
        val title: String,
        val priceTag: String,
        val conditionDescription: String,
        val formattedSummaryText: String
    )

    fun buildCard(store: DataStore, itemId: String, userAskingPrice: Double): ResaleCardLayout? {
        val entry = store.loadAll().find { it.id == itemId } ?: return null
        val days = entry.getDaysOwned()

        val desc = "【95新·个人自用闲置】" + entry.brand + "\n" +
                "• 购入时间：已持有 " + days + " 天\n" +
                "• 分类：" + entry.category + "\n" +
                "• 原价：¥" + String.format("%.2f", entry.price) + "\n" +
                "• 发票凭证：" + (if (entry.receiptPath.isNotBlank()) "电子发票齐全" else "箱说全") + "\n" +
                "• 备注说明：" + entry.notes.ifBlank { "爱护极佳，功能完好无暗病，支持验货。" }

        return ResaleCardLayout(
            title = entry.brand,
            priceTag = "¥" + String.format("%.0f", userAskingPrice),
            conditionDescription = "个人一手闲置",
            formattedSummaryText = desc
        )
    }
}