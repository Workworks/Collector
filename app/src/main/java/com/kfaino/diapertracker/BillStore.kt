package com.kfaino.diapertracker

import android.content.Context
import org.json.JSONArray
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class BillStore(context: Context) {
    private val prefs = context.getSharedPreferences("collector_data", Context.MODE_PRIVATE)

    data class CurrencySummary(
        val currency: String,
        val postedExpenseMinor: Long,
        val postedIncomeMinor: Long,
        val pendingMinor: Long,
        val count: Int
    )

    fun loadAll(): List<BillRecord> = synchronized(CompleteBackupStore.transactionLock) {
        BillJsonCodec.decode(prefs.getString(KEY, "[]") ?: "[]")
            .sortedWith(compareByDescending<BillRecord> { it.occurredAt }.thenByDescending { it.createdAt })
    }

    fun addAll(records: List<BillRecord>) = synchronized(CompleteBackupStore.transactionLock) {
        if (records.isEmpty()) return
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val current = BillJsonCodec.decode(raw)
        val array = JSONArray(raw)
        val ids = current.mapTo(mutableSetOf(), BillRecord::id)
        require(records.all { ids.add(it.id) }) { "批量账单包含重复标识" }
        records.forEach { array.put(it.toJson()) }
        check(prefs.edit().putString(KEY, array.toString()).commit()) { "批量账单写入失败" }
    }

    fun upsert(record: BillRecord) = synchronized(CompleteBackupStore.transactionLock) {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        BillJsonCodec.decode(raw)
        val source = JSONArray(raw)
        val output = JSONArray()
        var found = false
        for (index in 0 until source.length()) {
            val existing = source.getJSONObject(index)
            if (existing.getString("id") == record.id) {
                output.put(BillJsonCodec.mergeKnownFields(existing, record.copy(updatedAt = System.currentTimeMillis())))
                found = true
            } else output.put(existing)
        }
        if (!found) output.put(record.toJson())
        check(prefs.edit().putString(KEY, output.toString()).commit()) { "账单保存失败" }
    }

    fun delete(id: String) = synchronized(CompleteBackupStore.transactionLock) {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        BillJsonCodec.decode(raw)
        val source = JSONArray(raw)
        val output = JSONArray()
        for (index in 0 until source.length()) {
            val existing = source.getJSONObject(index)
            if (existing.getString("id") != id) output.put(existing)
        }
        check(prefs.edit().putString(KEY, output.toString()).commit()) { "账单删除失败" }
    }

    fun markPosted(id: String) = synchronized(CompleteBackupStore.transactionLock) {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        BillJsonCodec.decode(raw)
        val source = JSONArray(raw)
        val output = JSONArray()
        for (index in 0 until source.length()) {
            val existing = source.getJSONObject(index)
            if (existing.getString("id") == id) {
                existing.put("status", "posted").put("updated_at", System.currentTimeMillis())
            }
            output.put(existing)
        }
        check(prefs.edit().putString(KEY, output.toString()).commit()) { "账单状态更新失败" }
    }

    fun forMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): List<BillRecord> = loadAll().filter {
        YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month
    }

    fun summarize(records: List<BillRecord>): List<CurrencySummary> = records.groupBy(BillRecord::currency).map { (currency, list) ->
        CurrencySummary(
            currency = currency,
            postedExpenseMinor = list.filter { it.status == "posted" && it.direction == "expense" }.sumOf(BillRecord::amountMinor),
            postedIncomeMinor = list.filter { it.status == "posted" && it.direction == "income" }.sumOf(BillRecord::amountMinor),
            pendingMinor = list.filter { it.status == "pending" }.sumOf(BillRecord::amountMinor),
            count = list.size
        )
    }.sortedBy { it.currency }

    companion object { const val KEY = "bills_v1" }
}
