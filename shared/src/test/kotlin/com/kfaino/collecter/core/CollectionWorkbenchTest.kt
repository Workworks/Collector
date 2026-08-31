package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CollectionWorkbenchTest {
    private fun document()=JSONObject("""{"entries":[{"id":"asset","brand":"咖啡机","loc":"厨房","future":{"keep":true}}],"inbox":[{"id":"receipt","original":"合计 1299.00 2026年8月31日 型号: CM-30"}]}""")
    @Test fun captureSuggestionsSearchLinksAndSavedSearchRoundTrip() {
        val original=document()
        assertEquals(1,CollectionWorkbench.duplicates(original,"合计 1299.00 2026年8月31日 型号: CM-30").size)
        assertEquals("1299.00",CollectionWorkbench.suggestions("合计 1299.00 2026年8月31日 型号: CM-30").getString("amount"))
        assertEquals("2026-08-31",CollectionWorkbench.suggestions("2026年8月31日").getString("date"))
        assertFalse(CollectionWorkbench.suggestions("2026-02-31").has("date"))
        var next=CollectionWorkbench.apply(original,JSONObject("""{"op":"batch","refs":["entries:asset"],"patch":{"tags":["咖啡"],"_responsible":"家人"}}"""))
        assertTrue(next.getJSONArray("entries").getJSONObject(0).getJSONObject("future").getBoolean("keep"))
        assertEquals("entries:asset",CollectionWorkbench.search(next,"咖啡 家人","厨房").single().reference)
        next=CollectionWorkbench.apply(next,JSONObject("""{"op":"link","left":"entries:asset","right":"inbox:receipt"}"""))
        assertEquals(listOf("inbox:receipt"),WorkspaceRecords.related(next.getJSONArray("links"),"entries:asset"))
        next=CollectionWorkbench.apply(next,JSONObject("""{"op":"save-search","query":"咖啡","location":"厨房"}"""))
        val restored=BackupDocument.parse(next.toString())
        assertEquals(1,restored.getJSONArray("saved_searches").length())
        assertEquals("厨房",restored.getJSONArray("saved_searches").getJSONObject(0).getString("location"))
        assertFalse(original.has("links"))
    }
    @Test fun invalidBatchIsAllOrNothingAndLifecycleTransitionsHaveHistory() {
        val root=document();val before=root.toString()
        assertThrows(IllegalArgumentException::class.java) {CollectionWorkbench.apply(root,JSONObject("""{"op":"batch","refs":["entries:asset","entries:missing"],"patch":{"loc":"changed"}}"""))}
        assertEquals(before,root.toString())
        var next=CollectionWorkbench.apply(root,JSONObject("""{"op":"life","refs":["entries:asset"],"action":"lend","person":"阿明"}"""),now=1000)
        assertEquals("lent",next.getJSONArray("entries").getJSONObject(0).getString("_lifeState"))
        assertThrows(IllegalArgumentException::class.java) {CollectionWorkbench.apply(next,JSONObject("""{"op":"life","refs":["entries:asset"],"action":"sell"}"""))}
        next=CollectionWorkbench.apply(next,JSONObject("""{"op":"life","refs":["entries:asset"],"action":"return"}"""),now=2000)
        next=CollectionWorkbench.apply(next,JSONObject("""{"op":"life","refs":["entries:asset"],"action":"maintenance","nextAt":6000,"note":"清洁完成"}"""),now=3000)
        val asset=next.getJSONArray("entries").getJSONObject(0)
        assertEquals(3,asset.getJSONArray("_lifeEvents").length())
        assertEquals(6000,asset.getLong("_nextActionAt"))
        assertEquals("清洁完成",asset.getJSONArray("_lifeEvents").getJSONObject(2).getString("note"))
    }
    @Test fun ledgerReferencesDoNotCollideAndWritesAdvanceRevision() {
        val root=document().put("ledger_entries",JSONObject("""{"entries_ledger_other":[{"id":"asset","brand":"另一账本","_updatedAt":50}]}"""))
        val next=CollectionWorkbench.apply(root,JSONObject("""{"op":"batch","refs":["entries_ledger_other:asset"],"patch":{"loc":"书房"}}"""),now=100)
        assertEquals("厨房",next.getJSONArray("entries").getJSONObject(0).getString("loc"))
        val changed=next.getJSONObject("ledger_entries").getJSONArray("entries_ledger_other").getJSONObject(0)
        assertEquals("书房",changed.getString("loc"));assertEquals(100,changed.getLong("_updatedAt"))
    }
    @Test fun twentyThousandRecordSearchMeasuredWithoutClaimingDeviceLatency() {
        val entries=JSONArray()
        repeat(20000) {entries.put(JSONObject().put("id","id-$it").put("brand","物品 $it").put("loc",if(it%100==0) "厨房" else "书房"))}
        val root=JSONObject().put("entries",entries)
        val started=System.nanoTime();val found=CollectionWorkbench.search(root,"物品","厨房")
        val elapsed=(System.nanoTime()-started)/1000000
        assertEquals(200,found.size)
        println("PERF workbench search: records=20000 results=200 elapsedMs=$elapsed (JVM only)")
    }
}
