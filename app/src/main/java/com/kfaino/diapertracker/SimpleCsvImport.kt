package com.kfaino.diapertracker

object SimpleCsvImport {
    data class Result(val entries: List<Entry>, val rejectedRows: Int)

    fun parse(text: String, now: Long = System.currentTimeMillis()): Result {
        val rows = text.lineSequence().filter { it.isNotBlank() }.map(::parseRow).toList()
        if (rows.isEmpty()) return Result(emptyList(), 0)
        val normalizedHeader = rows.first().map { it.trim().lowercase() }
        val hasHeader = normalizedHeader.any { it in setOf("name", "名称", "物品", "brand") }
        val header = if (hasHeader) normalizedHeader else listOf("name", "category", "location", "notes", "qty")
        val data = if (hasHeader) rows.drop(1) else rows
        val result = mutableListOf<Entry>()
        var rejected = 0
        for (row in data) {
            fun value(vararg names: String): String {
                val index = names.asSequence().map { header.indexOf(it) }.firstOrNull { it >= 0 } ?: -1
                return row.getOrNull(index)?.trim().orEmpty()
            }
            val name = value("name", "名称", "物品", "brand")
            if (name.isBlank()) { rejected++; continue }
            result += Entry(
                brand = name,
                category = value("category", "分类").ifBlank { "通用" },
                location = value("location", "位置").ifBlank { "未整理" },
                notes = value("notes", "备注"),
                qty = value("qty", "数量").toIntOrNull()?.takeIf { it > 0 } ?: 1,
                ts = now + result.size,
                purchaseDate = now
            )
        }
        return Result(result, rejected)
    }

    private fun parseRow(line: String): List<String> {
        val result = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { result += cell.toString(); cell.clear() }
                else -> cell.append(c)
            }
            i++
        }
        result += cell.toString()
        return result
    }
}
