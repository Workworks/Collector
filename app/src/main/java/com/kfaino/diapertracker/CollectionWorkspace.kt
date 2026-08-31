package com.kfaino.diapertracker

import android.content.Context
import com.kfaino.collecter.core.WorkspaceRecords
import org.json.JSONArray
import org.json.JSONObject

class CollectionWorkspace(context: Context) {
    private val prefs = context.getSharedPreferences("collector_data", Context.MODE_PRIVATE)
    fun records(collection: String): JSONArray {
        val key = CompleteBackupStore.collectionKeys[collection] ?: error("不支持的数据集合")
        return JSONArray(prefs.getString(key, "[]") ?: "[]")
    }
    fun save(collection: String, list: JSONArray) = JsonCollectionWriter.save(prefs,
        CompleteBackupStore.collectionKeys[collection] ?: error("不支持的数据集合"), list)
    fun upsert(collection: String, record: JSONObject): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val list = records(collection)
        val id = record.getString("id")
        val idx = (0 until list.length()).firstOrNull { list.getJSONObject(it).getString("id") == id }
        if (idx == null) list.put(record) else list.put(idx, record)
        save(collection, list)
    }
    fun remove(collection: String, id: String): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val list = records(collection)
        save(collection, JSONArray((0 until list.length()).map { list.getJSONObject(it) }.filter { it.getString("id") != id }))
    }
    fun addText(text: String) = upsert("inbox", WorkspaceRecords.inbox(text))
    fun finishOcr(id: String, requestId: String, text: String?, failure: String?): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val list = records("inbox")
        val index = (0 until list.length()).firstOrNull { list.getJSONObject(it).optString("id") == id }
        if (index != null) WorkspaceRecords.ocrResult(list.getJSONObject(index), requestId, text, failure)?.let {
            list.put(index, it)
            save("inbox", list)
        }
    }
    fun link(left: String, right: String): Unit = synchronized(CompleteBackupStore.transactionLock) {
        save("links", WorkspaceRecords.link(records("links"), left, right))
    }

    fun document(): JSONObject = JSONObject().apply {
        val entries = JSONArray()
        for ((key, value) in prefs.all) if ((key == "entries_v4" || key.startsWith("entries_ledger_")) && value is String) {
            val arr = JSONArray(value)
            for (i in 0 until arr.length()) entries.put(arr.getJSONObject(i))
        }
        put("entries", entries)
        for (collection in CompleteBackupStore.collectionKeys.keys) put(collection, records(collection))
    }

    fun find(reference: String): JSONObject? {
        val collection = reference.substringBefore(':'); val id = reference.substringAfter(':')
        val arr = document().optJSONArray(collection) ?: return null
        return (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
    }

    fun related(reference: String): List<String> {
        val result = WorkspaceRecords.related(records("links"), reference).toMutableSet()
        for (collection in listOf("ideas", "clippings")) {
            val arr = records(collection)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val ref = "$collection:${item.getString("id")}" 
                val ids = item.optJSONArray("linked_ids") ?: continue
                for (j in 0 until ids.length()) {
                    val asset = "entries:${ids.getString(j)}"
                    if (ref == reference) result.add(asset)
                    if (asset == reference) result.add(ref)
                }
            }
        }
        return result.toList()
    }

    fun unlink(left: String, right: String): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val edges = records("links")
        save("links", JSONArray((0 until edges.length()).map { edges.getJSONObject(it) }.filter {
            setOf(it.optString("left"), it.optString("right")) != setOf(left, right)
        }))
        for ((recordRef, other) in listOf(left to right, right to left)) {
            val collection = recordRef.substringBefore(':')
            if (collection !in listOf("ideas", "clippings") || !other.startsWith("entries:")) continue
            val item = find(recordRef) ?: continue
            val ids = item.optJSONArray("linked_ids") ?: continue
            item.put("linked_ids", JSONArray((0 until ids.length()).map { ids.getString(it) }.filter { it != other.substringAfter(':') }))
            upsert(collection, item)
        }
    }
    fun title(item: JSONObject): String = sequenceOf("title", "brand", "name", "content", "original").map { item.optString(it) }
        .firstOrNull { it.isNotBlank() }?.take(80) ?: "未命名记录"
}
