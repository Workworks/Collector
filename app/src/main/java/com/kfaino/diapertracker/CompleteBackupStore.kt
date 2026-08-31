package com.kfaino.diapertracker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kfaino.collecter.core.BackupDocument
import com.kfaino.collecter.core.WireAliases
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Exports stored JSON, not a lossy projection of Entry. Credentials and hot-patch settings are excluded. */
class CompleteBackupStore(private val context: Context, private val entriesKey: String) {
    private val prefs = context.getSharedPreferences("collector_data", Context.MODE_PRIVATE)
    private val ledgers = context.getSharedPreferences("collector_ledgers_v1", Context.MODE_PRIVATE)
    private val kits = context.getSharedPreferences("collector_kits_prefs", Context.MODE_PRIVATE)

    companion object {
        internal val transactionLock = Any()
        val collectionKeys = linkedMapOf(
            "houses" to "houses_v1", "vouchers" to "vault_vouchers_v1", "identity_docs" to "vault_identity_docs_v1",
            "medicines" to "vault_medicines_v1", "foods" to "vault_foods_v1", "honors" to "vault_honors_v1",
            "wardrobe" to "vault_wardrobe_v1", "emergency" to "vault_emergency_v1", "tools" to "vault_tools_maintenance_v1",
            "plants" to "vault_plants_care_v1", "pets" to "vault_pets_care_v1", "books" to "vault_books_v1",
            "beverages" to "vault_beverage_tea_v1", "ideas" to "vault_ideas_v1", "clippings" to "vault_clippings_v1",
            "inbox" to "collection_inbox_v1", "links" to "collection_links_v1", "reminders" to "collection_reminders_v1",
            "saved_searches" to "collection_saved_searches_v1")
    }

    fun exportJson(): String = synchronized(transactionLock) {
        val root = JSONObject(prefs.getString("backup_extra_v2", "{}") ?: "{}")
        root.put("schemaVersion", 2).put("version", 5).put("timestamp", System.currentTimeMillis())
        root.put("entries", JSONArray(prefs.getString("entries_v4", null) ?: prefs.getString("entries_v3", null)
            ?: prefs.getString("entries_v2", "[]") ?: "[]"))
        root.put("categories", JSONArray(prefs.getString("custom_categories_v2", "[]") ?: "[]"))
        for ((collection, key) in collectionKeys) root.put(collection, JSONArray(prefs.getString(key, "[]") ?: "[]"))
        val ledgerEntries = JSONObject()
        for ((key, value) in prefs.all) if (key.matches(Regex("entries_ledger_[A-Za-z0-9_-]+")) && value is String) {
            ledgerEntries.put(key, JSONArray(value))
        }
        root.put("ledger_entries", ledgerEntries)
        root.put("ledgers", JSONArray(ledgers.getString("ledgers_list_json", "[]") ?: "[]"))
        root.put("active_ledger", ledgers.getString("current_ledger_id", "default"))
        root.put("kits", JSONArray(kits.getString("custom_kits_v1", "[]") ?: "[]"))
        val deleted = root.optJSONObject("_tombstones") ?: JSONObject()
        for ((collection, source) in listOf("ledgers" to ledgers, "kits" to kits)) {
            val metadata = JSONObject(source.getString("backup_extra_v2", "{}") ?: "{}")
            metadata.optJSONObject("_tombstones")?.optJSONObject(collection)?.let { tombstones ->
                val merged = deleted.optJSONObject(collection) ?: JSONObject()
                for (id in tombstones.keys()) merged.put(id, maxOf(merged.optLong(id), tombstones.getLong(id)))
                deleted.put(collection, merged)
            }
        }
        root.put("_tombstones", deleted)
        val roots = listOfNotNull(context.filesDir, context.getExternalFilesDir(null))
        return BackupDocument.attachFiles(root, roots).toString(2)
    }

    fun preview(text: String) = BackupDocument.preview(text)

    fun importJson(text: String): Boolean = synchronized(transactionLock) {
        val oldMain = prefs.all.toMap()
        val oldLedgers = ledgers.all.toMap()
        val oldKits = kits.all.toMap()
        var touched = false
        return try {
            val validated = WireAliases.convert(BackupDocument.parse(text))
            val active = if (validated.has("active_ledger")) validated.getString("active_ledger") else "default"
            require(active.matches(Regex("[A-Za-z0-9_-]+"))) { "无效账本标识" }
            val root = BackupDocument.restoreFiles(validated, context.filesDir)
            val editor = prefs.edit()
            if (root.has("entries")) editor.putString(if (root.optInt("schemaVersion", 1) >= 2) "entries_v4" else entriesKey, root.getJSONArray("entries").toString())
            if (root.has("categories")) editor.putString("custom_categories_v2", root.getJSONArray("categories").toString())
            for ((collection, key) in collectionKeys) if (root.has(collection)) editor.putString(key, root.getJSONArray(collection).toString())
            root.optJSONObject("ledger_entries")?.let { all ->
                for (key in all.keys()) editor.putString(key, all.getJSONArray(key).toString())
            }
            val extra = JSONObject(root.toString())
            for (key in BackupDocument.collections + listOf("categories", "ledger_entries", "active_ledger", "assets")) extra.remove(key)
            editor.putString("backup_extra_v2", extra.toString())
            // Journal the old preference sets before touching any of the three stores.
            val journal = File(context.filesDir, "restore-journal.json")
            val snapshot = JSONObject().put("main", encodePreferences(oldMain)).put("ledgers", encodePreferences(oldLedgers)).put("kits", encodePreferences(oldKits))
            BackupDocument.atomicWrite(journal, snapshot.toString().toByteArray())
            touched = true
            check(editor.commit()) { "资产数据写入失败" }
            if (root.has("ledgers")) check(ledgers.edit().putString("ledgers_list_json", root.getJSONArray("ledgers").toString())
                .putString("current_ledger_id", active).putString("backup_extra_v2", extra.toString()).commit()) { "账本写入失败" }
            if (root.has("kits")) check(kits.edit().putString("custom_kits_v1", root.getJSONArray("kits").toString()).putString("backup_extra_v2", extra.toString()).commit()) { "套装写入失败" }
            check(journal.delete()) { "恢复事务无法完成" }
            true
        } catch (e: Exception) {
            Log.e("CompleteBackupStore", "恢复失败，回滚旧数据", e)
            if (touched) {
                check(restore(prefs, oldMain) && restore(ledgers, oldLedgers) && restore(kits, oldKits)) { "回滚失败，请保留恢复日志" }
                val journal = File(context.filesDir, "restore-journal.json")
                if (journal.exists() && !journal.delete()) Log.w("CompleteBackupStore", "回滚完成但事务日志未删除")
            }
            false
        }
    }

    private fun restore(target: SharedPreferences, values: Map<String, *>): Boolean {
        val editor = target.edit().clear()
        for ((key, value) in values) when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
        return editor.commit()
    }

    private fun encodePreferences(values: Map<String, *>): JSONObject = JSONObject().apply {
        for ((key, value) in values) {
            val type = when (value) {
                is String -> "string"
                is Boolean -> "boolean"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is Set<*> -> "set"
                else -> error("不支持的设置类型")
            }
            put(key, JSONObject().put("type", type).put("value", if (value is Set<*>) JSONArray(value) else value))
        }
    }

    fun recoverInterruptedRestore(): Unit = synchronized(transactionLock) {
        val journal = File(context.filesDir, "restore-journal.json")
        if (!journal.exists()) return
        val root = JSONObject(journal.readText())
        fun values(name: String): Map<String, Any> {
            val obj = root.getJSONObject(name)
            return obj.keys().asSequence().associateWith {
                val typed = obj.getJSONObject(it)
                when (typed.getString("type")) {
                    "string" -> typed.getString("value")
                    "boolean" -> typed.getBoolean("value")
                    "int" -> typed.getInt("value")
                    "long" -> typed.getLong("value")
                    "float" -> typed.getDouble("value").toFloat()
                    "set" -> typed.getJSONArray("value").let { arr -> (0 until arr.length()).map { i -> arr.getString(i) }.toSet() }
                    else -> error("恢复日志类型错误")
                }
            }
        }
        check(restore(prefs, values("main")) && restore(ledgers, values("ledgers")) && restore(kits, values("kits"))) { "未完成恢复事务回滚失败" }
        check(journal.delete()) { "恢复日志清理失败" }
        Log.w("CompleteBackupStore", "已回滚上次中断的恢复事务")
    }
}
