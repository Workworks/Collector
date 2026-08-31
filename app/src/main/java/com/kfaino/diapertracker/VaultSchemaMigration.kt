package com.kfaino.diapertracker

import android.content.SharedPreferences
import com.kfaino.collecter.core.WireAliases
import org.json.JSONArray
import org.json.JSONObject

/** One atomic migration; legacy keys stay untouched for recovery, never replay after deletion. */
object VaultSchemaMigration {
    private const val MARKER = "vault_schema_457_migrated"
    private val oldKeys = mapOf("tools" to "vault_tools_v1", "plants" to "vault_plants_v1",
        "pets" to "vault_pets_v1", "beverages" to "vault_beverage_v1")

    fun migrate(prefs: SharedPreferences) = synchronized(CompleteBackupStore.transactionLock) {
        if (!prefs.getBoolean(MARKER, false)) {
            val root = JSONObject()
            for ((collection, key) in CompleteBackupStore.collectionKeys) {
                val legacy = oldKeys[collection]?.let { prefs.getString(it, null) }
                val rows = linkedMapOf<String, JSONObject>()
                for (raw in listOfNotNull(legacy, prefs.getString(key, null))) {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val row = arr.getJSONObject(i)
                        val id = row.optString("id").ifBlank { "missing-id-${rows.size}" }
                        val merged = rows[id] ?: JSONObject()
                        for (field in row.keys()) merged.put(field, row.get(field))
                        rows[id] = merged
                    }
                }
                if (rows.isNotEmpty()) root.put(collection, JSONArray(rows.values.toList()))
            }
            val normalized = WireAliases.convert(root)
            val editor = prefs.edit()
            for ((collection, key) in CompleteBackupStore.collectionKeys) {
                normalized.optJSONArray(collection)?.let { editor.putString(key, it.toString()) }
            }
            check(editor.putBoolean(MARKER, true).commit()) { "旧版收藏数据迁移保存失败；原始键未删除" }
        }
    }
}
