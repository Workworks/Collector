package com.kfaino.diapertracker

import android.content.SharedPreferences
import com.kfaino.collecter.core.SnapshotSync
import org.json.JSONArray
import org.json.JSONObject

/** Preserve future fields and write edit versions/deletion tombstones with each collection update. */
object JsonCollectionWriter {
    fun save(prefs: SharedPreferences, key: String, records: JSONArray, collectionName: String? = null): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val collection = collectionName ?: CompleteBackupStore.collectionKeys.entries.firstOrNull { it.value == key }?.key ?: "entries"
        val metadataKey = if (key.startsWith("entries_ledger_")) key else collection
        val extra = JSONObject(prefs.getString("backup_extra_v2", "{}") ?: "{}")
        val allDeleted = extra.optJSONObject("_tombstones") ?: JSONObject()
        val before = JSONObject().put(collection, JSONArray(prefs.getString(key, "[]") ?: "[]"))
            .put("_tombstones", JSONObject().put(collection, allDeleted.optJSONObject(metadataKey) ?: JSONObject()))
        val next = SnapshotSync.recordChanges(before, JSONObject().put(collection, records))
        allDeleted.put(metadataKey, next.getJSONObject("_tombstones").getJSONObject(collection))
        extra.put("_tombstones", allDeleted)
        check(prefs.edit().putString(key, next.getJSONArray(collection).toString())
            .putString("backup_extra_v2", extra.toString()).commit()) { "数据保存失败" }
    }
}
