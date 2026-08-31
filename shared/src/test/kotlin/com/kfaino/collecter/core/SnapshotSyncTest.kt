package com.kfaino.collecter.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SnapshotSyncTest {
    @Test fun editsKeepFutureFieldsAndDeletionCannotResurrect() {
        val original = JSONObject("""{"entries":[{"id":"x","brand":"old","future":{"a":1},"_updatedAt":10}]}""")
        val edited = SnapshotSync.recordChanges(original, JSONObject("""{"entries":[{"id":"x","brand":"new"}]}"""), 20)
        assertEquals(1, edited.getJSONArray("entries").getJSONObject(0).getJSONObject("future").getInt("a"))
        val deleted = SnapshotSync.recordChanges(edited, JSONObject("""{"entries":[]} """), 30)
        val merged = SnapshotSync.merge(deleted.toString(), edited.toString())
        assertEquals(0, merged.document.getJSONArray("entries").length())
        assertEquals(30, merged.document.getJSONObject("_tombstones").getJSONObject("entries").getLong("x"))
        assertEquals(0, SnapshotSync.merge(merged.document.toString(), original.toString()).document.getJSONArray("entries").length())
    }

    @Test fun sameVersionConflictsConvergeAndKeepBothVariants() {
        val a = """{"entries":[{"id":"x","brand":"a","_updatedAt":10}]}"""
        val b = """{"entries":[{"id":"x","brand":"b","_updatedAt":10}]}"""
        val ab = SnapshotSync.merge(a, b)
        val ba = SnapshotSync.merge(b, a)
        assertEquals(SnapshotSync.canonical(ab.document), SnapshotSync.canonical(ba.document))
        assertEquals(1, ab.conflicts)
        val again = SnapshotSync.merge(ab.document.toString(), ab.document.toString())
        assertEquals(0, again.conflicts)
        assertEquals(1, again.document.getJSONArray("entries").length())
    }

    @Test fun everyCollectionAndUnrecognizedFieldSurvives() {
        val a = JSONObject().put("entries", org.json.JSONArray())
        val b = JSONObject()
        for (collection in BackupDocument.collections) b.put(collection, org.json.JSONArray()
            .put(JSONObject().put("id", collection).put("future", JSONObject().put("nested", 42))))
        val merged = SnapshotSync.merge(a.toString(), b.toString()).document
        for (collection in BackupDocument.collections) assertEquals(42,
            merged.getJSONArray(collection).getJSONObject(0).getJSONObject("future").getInt("nested"))
    }

    @Test fun identityAndEntryWireAliasesMatchBothPlatforms() {
        val android = JSONObject("""{"entries":[{"id":"x","is_sub":true,"sub_cyc":"MONTHLY","img_p":"","ret_sp":3}],"identity_docs":[{"id":"doc","mem":"家人","dtype":"passport","dnum":"test","exp_d":123}]}""")
        val desktop = WireAliases.convert(android, true)
        assertEquals("test", desktop.getJSONArray("identity_docs").getJSONObject(0).getString("certNumber"))
        assertEquals("家人", desktop.getJSONArray("identity_docs").getJSONObject(0).getString("member"))
        assertEquals(3, desktop.getJSONArray("entries").getJSONObject(0).getInt("retired_sold_price"))
        assertEquals(SnapshotSync.canonical(android), SnapshotSync.canonical(WireAliases.convert(desktop)))
    }
}
