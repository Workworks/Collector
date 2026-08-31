package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject

/** Collection-level lossless merge. Deletions win; recreating a record requires a new ID. */
object SnapshotSync {
    data class Result(val document: JSONObject, val inserted: Int, val updated: Int, val preserved: Int,
        val vaultChanges: Int, val conflicts: Int, val deleted: Int)

    private fun records(array: JSONArray?): Map<String, JSONObject> {
        if (array == null) return emptyMap()
        return (0 until array.length()).associate { i ->
            val obj = JSONObject(array.getJSONObject(i).toString())
            val id = obj.optString("id").ifBlank { "legacy-" + BackupDocument.sha256(canonical(obj).toByteArray()).take(32) }
            obj.put("id", id)
            id to obj
        }
    }

    fun recordChanges(previous: JSONObject, edited: JSONObject, now: Long = System.currentTimeMillis()): JSONObject {
        val old = WireAliases.convert(previous)
        val next = WireAliases.convert(edited)
        val result = JSONObject(old.toString())
        for (key in next.keys()) result.put(key, next.get(key))
        val tombstones = JSONObject(old.optJSONObject("_tombstones")?.toString() ?: "{}")
        for (key in BackupDocument.collections) {
            if (!next.has(key)) continue
            val before = records(old.optJSONArray(key))
            val after = records(next.getJSONArray(key))
            val deleted = tombstones.optJSONObject(key) ?: JSONObject()
            for (id in before.keys - after.keys) deleted.put(id, now)
            tombstones.put(key, deleted)
            val arr = JSONArray()
            for ((id, value) in after) {
                if (deleted.has(id)) continue
                val original = before[id]
                val combined = JSONObject(original?.toString() ?: "{}")
                for (field in value.keys()) if (field != "_updatedAt") combined.put(field, value.get(field))
                val compare = JSONObject(combined.toString()).apply { remove("_updatedAt") }
                val oldCompare = JSONObject(original?.toString() ?: "{}").apply { remove("_updatedAt") }
                val timestamp = original?.optLong("_updatedAt", original.optLong("ts", 0L)) ?: 0L
                combined.put("_updatedAt", if (canonical(compare) == canonical(oldCompare)) timestamp else maxOf(now, timestamp + 1))
                arr.put(combined)
            }
            result.put(key, arr)
        }
        result.put("_tombstones", tombstones).put("schemaVersion", 2)
        return result
    }

    fun merge(localText: String, remoteText: String): Result {
        val local = WireAliases.convert(BackupDocument.parse(localText))
        val remote = WireAliases.convert(BackupDocument.parse(remoteText))
        val result = JSONObject(local.toString())
        for (key in remote.keys()) if (!result.has(key)) result.put(key, remote.get(key))
        val assets = JSONObject(local.optJSONObject("assets")?.toString() ?: "{}")
        remote.optJSONObject("assets")?.let { other -> for (key in other.keys()) assets.put(key, other.get(key)) }
        result.put("assets", assets)
        val tombstones = JSONObject()
        val conflictLog = JSONObject(local.optJSONObject("_conflicts")?.toString() ?: "{}")
        remote.optJSONObject("_conflicts")?.let { other -> for (key in other.keys()) conflictLog.put(key, other.get(key)) }
        var inserted = 0; var updated = 0; var preserved = 0; var vaultChanges = 0; var conflicts = 0; var deletedCount = 0
        for (key in BackupDocument.collections) {
            if (!local.has(key) && !remote.has(key)) continue
            val a = records(local.optJSONArray(key)); val b = records(remote.optJSONArray(key))
            val dead = JSONObject()
            for (source in listOf(local, remote)) source.optJSONObject("_tombstones")?.optJSONObject(key)?.let { map ->
                for (id in map.keys()) dead.put(id, maxOf(dead.optLong(id, 0L), map.getLong(id)))
            }
            tombstones.put(key, dead)
            val arr = JSONArray()
            for (id in (a.keys + b.keys).sorted()) {
                if (dead.has(id)) { deletedCount++; continue }
                val left = a[id]; val right = b[id]
                val chosen = when {
                    left == null -> right!!
                    right == null -> left
                    canonical(left) == canonical(right) -> left
                    else -> {
                        val modern = left.has("_updatedAt") && right.has("_updatedAt")
                        val ta = if (modern) left.getLong("_updatedAt") else left.optLong("ts", 0L)
                        val tb = if (modern) right.getLong("_updatedAt") else right.optLong("ts", 0L)
                        // Save both variants for inspection. Timestamp + canonical text gives a stable winner.
                        val variants = listOf(canonical(left), canonical(right)).sorted()
                        val conflictId = BackupDocument.sha256((key + id + variants.joinToString()).toByteArray())
                        if (!conflictLog.has(conflictId)) {
                            conflicts++
                            conflictLog.put(conflictId, JSONObject().put("collection", key).put("id", id)
                                .put("variants", JSONArray(variants.map { JSONObject(it) })))
                        }
                        val newer = if (ta > tb || ta == tb && canonical(left) >= canonical(right)) left else right
                        val older = if (newer === left) right else left
                        JSONObject(older.toString()).apply { for (field in newer.keys()) put(field, newer.get(field)) }
                    }
                }
                arr.put(chosen)
                if (key == "entries") {
                    if (left == null) inserted++ else if (canonical(left) != canonical(chosen)) updated++ else preserved++
                } else if (left == null || canonical(left) != canonical(chosen)) vaultChanges++
            }
            result.put(key, arr)
        }
        val categories = linkedSetOf<String>()
        for (doc in listOf(local, remote)) doc.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) categories.add(arr.getString(i))
        }
        result.put("categories", JSONArray(categories.sorted()))
        result.put("_tombstones", tombstones).put("_conflicts", conflictLog).put("schemaVersion", 2)
        // Multi-ledger snapshots carry separate collections, never overwrite one ledger with another.
        val ledgerOutput = JSONObject()
        val ledgerA = local.optJSONObject("ledger_entries") ?: JSONObject()
        val ledgerB = remote.optJSONObject("ledger_entries") ?: JSONObject()
        for (key in (ledgerA.keys().asSequence().toSet() + ledgerB.keys().asSequence().toSet())) {
            val one = JSONObject().put("entries", ledgerA.optJSONArray(key) ?: JSONArray())
                .put("_tombstones", JSONObject().put("entries", local.optJSONObject("_tombstones")?.optJSONObject(key) ?: JSONObject()))
            val two = JSONObject().put("entries", ledgerB.optJSONArray(key) ?: JSONArray())
                .put("_tombstones", JSONObject().put("entries", remote.optJSONObject("_tombstones")?.optJSONObject(key) ?: JSONObject()))
            val ledgerResult = merge(one.toString(), two.toString())
            val mergedLedger = ledgerResult.document
            inserted += ledgerResult.inserted; updated += ledgerResult.updated
            preserved += ledgerResult.preserved; deletedCount += ledgerResult.deleted
            val ledgerConflicts = mergedLedger.getJSONObject("_conflicts")
            for (conflict in ledgerConflicts.keys()) {
                val conflictKey = "$key:$conflict"
                if (!conflictLog.has(conflictKey)) conflicts++
                conflictLog.put(conflictKey, ledgerConflicts.getJSONObject(conflict).put("collection", key))
            }
            ledgerOutput.put(key, mergedLedger.getJSONArray("entries"))
            tombstones.put(key, mergedLedger.getJSONObject("_tombstones").getJSONObject("entries"))
        }
        if (ledgerOutput.length() > 0) result.put("ledger_entries", ledgerOutput)
        return Result(result, inserted, updated, preserved, vaultChanges, conflicts, deletedCount)
    }

    fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canonical(value.get(it)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is String -> JSONObject.quote(value)
        else -> value.toString()
    }
}
