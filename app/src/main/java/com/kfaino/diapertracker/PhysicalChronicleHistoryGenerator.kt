package com.kfaino.diapertracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📜 跨世代家族物理文明编年史与口述历史生成器 (Physical Chronicle History Generator)
 */
object PhysicalChronicleHistoryGenerator {

    data class ChronicleChapter(
        val itemId: String,
        val itemName: String,
        val generationEpoch: String,
        val milestoneText: String
    )

    fun buildItemChronicle(store: DataStore, itemId: String): ChronicleChapter? {
        val entry = store.loadAll().find { it.id == itemId } ?: return null
        val dateStr = SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(entry.purchaseDate))
        val days = entry.getDaysOwned()

        val text = "【家族文明物质档案】" + entry.brand + "（" + entry.category + "）\n" +
                "• 始于：" + dateStr + " (已相伴守护 " + days + " 天)\n" +
                "• 传承印记：" + (entry.notes.ifBlank { "承载家族生活记忆，保存完好。" })

        return ChronicleChapter(entry.id, entry.brand, "2020年代·数字互联纪元", text)
    }
}