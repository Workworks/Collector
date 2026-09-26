package com.kfaino.collecter.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class BillDraft(
    val sourceLine: Int,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val direction: String,
    val category: String,
    val status: String,
    val recurrence: String,
    val occurredOn: LocalDate,
    val note: String,
    val sourceText: String
)

data class BillParseIssue(val sourceLine: Int, val sourceText: String, val message: String)

data class BillParseResult(val records: List<BillDraft>, val issues: List<BillParseIssue>)

/** Pure, local parser for the documented quick-entry format and common handwritten-note syntax. */
object BillQuickInputParser {
    private val dateHeader = Regex("^@?(?:(\\d{4})[-/])?(\\d{1,2})[-/](\\d{1,2})(?:\\s*[早中晚夜])?$")
    private val recommendedAmount = Regex("^([+-]?\\d+(?:\\.\\d{1,2})?)(USD|CNY|RMB|元|¥|￥)?$", RegexOption.IGNORE_CASE)
    private val explicitMoney = Regex("([+-]?\\d+(?:\\.\\d{1,2})?)\\s*(USD|CNY|RMB|元|¥|￥)", RegexOption.IGNORE_CASE)
    private val anyNumber = Regex("[+-]?\\d+(?:\\.\\d{1,2})?")
    private val compactAmount = Regex("^(.+?)([+-])(\\d+(?:\\.\\d{1,2})?)(.*)$")
    private val statusTags = setOf("#已入账", "#未入账")
    private val recurrenceTags = setOf("#每周", "#每月", "#每年")

    fun parse(text: String, today: LocalDate = LocalDate.now()): BillParseResult {
        var currentDate = today
        val records = mutableListOf<BillDraft>()
        val issues = mutableListOf<BillParseIssue>()
        text.lineSequence().take(500).forEachIndexed { index, raw ->
            val lineNumber = index + 1
            val line = raw.trim().trimEnd('。')
            if (line.isBlank()) return@forEachIndexed

            parseDateHeader(line.removePrefix("记账").trim(), today)?.let {
                currentDate = it
                return@forEachIndexed
            }
            if (isHeading(line)) return@forEachIndexed

            val parsed = parseRecommended(lineNumber, line, currentDate)
                ?: parseLegacy(lineNumber, line, currentDate)
            if (parsed != null) records += parsed
            else issues += BillParseIssue(lineNumber, line, "未找到明确金额，请改用“支出 20 餐饮 项目名”")
        }
        return BillParseResult(records, issues)
    }

    private fun parseDateHeader(value: String, today: LocalDate): LocalDate? {
        val match = dateHeader.matchEntire(value) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: today.year
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun isHeading(line: String): Boolean {
        val normalized = line.trimEnd(':', '：').trim()
        return normalized.endsWith("列表") || normalized == "记账" || normalized == "采购价格汇总"
    }

    private fun parseRecommended(lineNumber: Int, line: String, date: LocalDate): BillDraft? {
        val split = line.split(Regex("[;；]"), limit = 2)
        val body = split[0].trim()
        val note = split.getOrNull(1)?.trim().orEmpty()
        val tokens = body.split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.size < 4 || tokens[0] !in setOf("支出", "收入")) return null
        val amountMatch = recommendedAmount.matchEntire(tokens[1]) ?: return null
        val amount = toMinor(amountMatch.groupValues[1]) ?: return null
        val currency = normalizeCurrency(amountMatch.groupValues[2])
        val category = tokens[2]
        val tags = tokens.filter { it.startsWith('#') }.toSet()
        val title = tokens.drop(3).takeWhile { !it.startsWith('#') }.joinToString(" ").trim()
        if (title.isBlank()) return null
        return BillDraft(
            sourceLine = lineNumber,
            title = title,
            amountMinor = amount,
            currency = currency,
            direction = if (tokens[0] == "收入") "income" else "expense",
            category = category,
            status = if ("#未入账" in tags) "pending" else "posted",
            recurrence = when {
                "#每周" in tags -> "weekly"
                "#每月" in tags -> "monthly"
                "#每年" in tags -> "yearly"
                else -> "none"
            },
            occurredOn = date,
            note = note,
            sourceText = line
        )
    }

    private fun parseLegacy(lineNumber: Int, line: String, date: LocalDate): BillDraft? {
        val normalized = line.replace('：', ':').replace('（', '(').replace('）', ')')
        val colon = normalized.indexOf(':')
        val left = if (colon >= 0) normalized.substring(0, colon).trim() else normalized
        val right = if (colon >= 0) normalized.substring(colon + 1).trim() else normalized
        val amountInfo = legacyAmount(normalized, left, right) ?: return null
        var title = if (colon >= 0) left else compactAmount.matchEntire(normalized)?.groupValues?.get(1)?.trim().orEmpty()
        title = title.removePrefix("采购价格汇总").trim().ifBlank { inferFallbackTitle(normalized) }
        if (title.isBlank()) return null
        val negativeWasExpenseNotation = amountInfo.original.startsWith('-') && !containsIncomeKeyword(normalized)
        val noteParts = mutableListOf<String>()
        if (right.isNotBlank() && right != amountInfo.original) noteParts += right
        if (negativeWasExpenseNotation) noteParts += "原文负号按支出金额解析"
        return BillDraft(
            sourceLine = lineNumber,
            title = title,
            amountMinor = amountInfo.minor,
            currency = amountInfo.currency,
            direction = if (containsIncomeKeyword(normalized)) "income" else "expense",
            category = inferCategory(title, normalized),
            status = if (normalized.contains("未入账")) "pending" else "posted",
            recurrence = when {
                normalized.contains("每周") -> "weekly"
                normalized.contains("每月") || normalized.contains("下个月") -> "monthly"
                normalized.contains("每年") -> "yearly"
                else -> "none"
            },
            occurredOn = date,
            note = noteParts.distinct().joinToString("；"),
            sourceText = line
        )
    }

    private data class AmountInfo(val minor: Long, val currency: String, val original: String)

    private fun legacyAmount(full: String, left: String, right: String): AmountInfo? {
        if (full.contains("->") || full.contains("→")) {
            val before = full.split("->", "→", limit = 2).first()
            val candidate = anyNumber.find(before.substringAfter(':', before))?.value
            val minor = candidate?.let(::toMinor)
            if (minor != null) return AmountInfo(minor, if (full.contains("USD", true)) "USD" else "CNY", candidate)
        }
        val explicit = explicitMoney.findAll(right).toList().lastOrNull()
            ?: explicitMoney.findAll(full).toList().lastOrNull()
        if (explicit != null) {
            val raw = explicit.groupValues[1]
            val minor = toMinor(raw) ?: return null
            return AmountInfo(minor, normalizeCurrency(explicit.groupValues[2]), raw)
        }
        if (full.contains(':')) {
            val raw = anyNumber.find(right)?.value ?: return null
            val minor = toMinor(raw) ?: return null
            return AmountInfo(minor, if (right.contains("USD", true)) "USD" else "CNY", raw)
        }
        val compact = compactAmount.matchEntire(full) ?: return null
        if (left == full && compact.groupValues[1].isBlank()) return null
        val raw = compact.groupValues[3]
        return AmountInfo(toMinor(raw) ?: return null, if (full.contains("USD", true)) "USD" else "CNY", raw)
    }

    private fun toMinor(raw: String): Long? = runCatching {
        val value = BigDecimal(raw).abs()
        require(value > BigDecimal.ZERO && value <= BigDecimal("99999999.99"))
        value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }.getOrNull()

    private fun normalizeCurrency(raw: String): String = if (raw.equals("USD", true)) "USD" else "CNY"

    private fun containsIncomeKeyword(value: String): Boolean =
        listOf("收入", "退款", "报销", "收款", "返现").any(value::contains)

    private fun inferCategory(title: String, line: String): String {
        val text = "$title $line".lowercase()
        return when {
            listOf("chatgpt", "claude", "会员", "订阅", "续费").any(text::contains) -> "订阅"
            listOf("加油", "充电", "汽车", "停车", "公交", "打车", "配送").any(text::contains) -> "交通"
            listOf("鸡", "猪肉", "白菜", "餐", "饭", "菜", "水果", "外卖").any(text::contains) -> "餐饮"
            listOf("采购", "打包盒", "耗材").any(text::contains) -> "采购"
            containsIncomeKeyword(text) -> "退款"
            else -> "其他"
        }
    }

    private fun inferFallbackTitle(line: String): String = when {
        line.contains("加油") -> "汽车加油"
        line.contains("充电") -> "汽车充电"
        else -> "账单"
    }
}
