package com.kfaino.diapertracker

import com.kfaino.collecter.core.BillDraft
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.UUID

data class BillRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amountMinor: Long,
    val currency: String = "CNY",
    val direction: String = "expense",
    val category: String = "其他",
    val status: String = "posted",
    val recurrence: String = "none",
    val occurredAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val sourceText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "账单标识不能为空" }
        require(title.isNotBlank()) { "账单项目不能为空" }
        require(amountMinor > 0L) { "账单金额必须大于 0" }
        require(currency.matches(Regex("[A-Z]{3}"))) { "币种格式无效" }
        require(direction in setOf("expense", "income")) { "账单方向无效" }
        require(status in setOf("posted", "pending")) { "账单状态无效" }
        require(recurrence in setOf("none", "weekly", "monthly", "yearly")) { "账单周期无效" }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("amount_minor", amountMinor)
        .put("currency", currency)
        .put("direction", direction)
        .put("category", category)
        .put("status", status)
        .put("recurrence", recurrence)
        .put("occurred_at", occurredAt)
        .put("note", note)
        .put("source_text", sourceText)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    companion object {
        fun fromJson(value: JSONObject): BillRecord = BillRecord(
            id = value.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = value.getString("title").trim(),
            amountMinor = value.getLong("amount_minor"),
            currency = value.optString("currency", "CNY").uppercase(Locale.ROOT),
            direction = value.optString("direction", "expense"),
            category = value.optString("category", "其他").ifBlank { "其他" },
            status = value.optString("status", "posted"),
            recurrence = value.optString("recurrence", "none"),
            occurredAt = value.optLong("occurred_at", System.currentTimeMillis()),
            note = value.optString("note", ""),
            sourceText = value.optString("source_text", ""),
            createdAt = value.optLong("created_at", System.currentTimeMillis()),
            updatedAt = value.optLong("updated_at", System.currentTimeMillis())
        )

        fun fromDraft(value: BillDraft, now: Long = System.currentTimeMillis()): BillRecord = BillRecord(
            title = value.title,
            amountMinor = value.amountMinor,
            currency = value.currency,
            direction = value.direction,
            category = value.category,
            status = value.status,
            recurrence = value.recurrence,
            occurredAt = value.occurredOn.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            note = value.note,
            sourceText = value.sourceText,
            createdAt = now,
            updatedAt = now
        )
    }
}

object BillJsonCodec {
    fun encode(records: List<BillRecord>): String = JSONArray().apply { records.forEach { put(it.toJson()) } }.toString()

    fun decode(raw: String): List<BillRecord> {
        val array = JSONArray(raw)
        require(array.length() <= 100_000) { "账单数量超过上限" }
        val ids = mutableSetOf<String>()
        return (0 until array.length()).map { index ->
            val record = BillRecord.fromJson(array.getJSONObject(index))
            require(ids.add(record.id)) { "账单标识重复: ${record.id}" }
            record
        }
    }

    fun mergeKnownFields(original: JSONObject, record: BillRecord): JSONObject = JSONObject(original.toString()).apply {
        val known = record.toJson()
        for (key in known.keys()) put(key, known.get(key))
    }
}

object BillAmounts {
    fun parseMinor(text: String): Long? = runCatching {
        val amount = BigDecimal(text.trim())
        require(amount > BigDecimal.ZERO && amount <= BigDecimal("99999999.99"))
        amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }.getOrNull()

    fun format(minor: Long, currencyCode: String, signed: Boolean = false): String {
        val amount = BigDecimal.valueOf(minor, 2)
        val prefix = if (signed) "+" else ""
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.SIMPLIFIED_CHINESE).apply {
                currency = Currency.getInstance(currencyCode)
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            prefix + format.format(amount)
        } catch (e: IllegalArgumentException) {
            "$prefix$currencyCode ${amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
        }
    }
}
