package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupBoundary456Test {
    @Test fun exactUtf8LimitAcceptedAndOneByteOverRejected() {
        val prefix="{\"entries\":[],\"qa\":\""
        val suffix="\"}"
        val limit=BackupDocument.MAX_BYTES
        val text=prefix+"x".repeat(limit-prefix.length-suffix.length)+suffix
        assertEquals(limit,text.toByteArray().size)
        assertEquals(limit-prefix.length-suffix.length,BackupDocument.parse(text).getString("qa").length)
        assertThrows(IllegalArgumentException::class.java) {BackupDocument.parse(text+" ")}
        println("BOUNDARY: exact 64 MiB accepted; 64 MiB+1 rejected (JVM parser only)")
    }
    @Test fun repeatedBatchOperationsPreserveUnknownFieldsAndHistory() {
        var root=JSONObject().put("entries",JSONArray((0 until 1000).map {JSONObject().put("id","q$it").put("brand","QA $it").put("future","keep")}))
        val started=System.nanoTime()
        repeat(100) { turn ->
            val refs=JSONArray((0 until 10).map {"entries:q${turn*10+it}"})
            root=CollectionWorkbench.apply(root,JSONObject().put("op","batch").put("refs",refs).put("patch",JSONObject().put("loc","QA $turn")))
        }
        for(hit in CollectionWorkbench.records(root)) {
            assertEquals("keep",hit.record.getString("future"))
            assertEquals(1,hit.record.getJSONArray("_audit").length())
        }
        println("PRESSURE: records=1000 batches=100 totalUpdated=1000 elapsedMs=${(System.nanoTime()-started)/1_000_000} JVM only")
    }
}
