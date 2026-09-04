package com.kfaino.diapertracker

object QuickEntryFactory {
    data class Suggestion(val category: String, val location: String, val expiryDays: Int? = null)

    fun suggest(name: String, history: List<Entry>): Suggestion {
        val normalized = name.trim().lowercase()
        val previous = history.asReversed().firstOrNull { old ->
            old.brand.lowercase() == normalized || normalized.split(Regex("\\s+")).any { it.length > 1 && old.brand.lowercase().contains(it) }
        }
        if (previous != null) return Suggestion(previous.category, previous.location.ifBlank { "未整理" }, previous.expiryDate.takeIf { it > 0 }?.let { 30 })
        return when {
            listOf("药", "medicine", "维生素").any(normalized::contains) -> Suggestion("药品", "未整理", 365)
            listOf("食品", "牛奶", "面包", "水果", "food").any(normalized::contains) -> Suggestion("食品", "未整理", 7)
            listOf("证件", "保单", "合同", "护照").any(normalized::contains) -> Suggestion("贵重证件", "未整理")
            listOf("书", "book").any(normalized::contains) -> Suggestion("藏书", "未整理")
            listOf("手机", "电脑", "耳机", "数码").any(normalized::contains) -> Suggestion("数码", "未整理")
            else -> Suggestion("通用", "未整理")
        }
    }

    fun create(name: String, now: Long = System.currentTimeMillis(), history: List<Entry> = emptyList()): Entry? {
        val normalized = name.trim()
        if (normalized.isEmpty()) return null
        val suggestion = suggest(normalized, history)
        return Entry(
            brand = normalized,
            category = suggestion.category,
            qty = 1,
            unit = "件",
            location = suggestion.location,
            purchaseDate = now,
            ts = now
        )
    }
}
