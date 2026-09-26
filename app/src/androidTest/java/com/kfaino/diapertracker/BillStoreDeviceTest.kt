package com.kfaino.diapertracker

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class BillStoreDeviceTest {
    private fun isolated(): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "bill-qa-${UUID.randomUUID()}-"
        val dir = File(base.cacheDir, prefix).apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
            override fun getFilesDir() = dir
            override fun getExternalFilesDir(type: String?): File? = null
        }
    }

    @Test
    fun pendingStateUnknownFieldsAndBackupRoundTripRemainLossless() {
        val source = isolated()
        val target = isolated()
        val store = BillStore(source)
        val occurredAt = LocalDate.of(2026, 9, 26).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        store.addAll(listOf(
            BillRecord(id = "pending", title = "打包盒", amountMinor = 2882, status = "pending", occurredAt = occurredAt),
            BillRecord(id = "usd", title = "ChatGPT", amountMinor = 2000, currency = "USD", recurrence = "monthly", occurredAt = occurredAt)
        ))
        assertEquals(2, store.loadAll().size)
        assertEquals(2, store.summarize(store.loadAll()).size)
        store.markPosted("pending")
        assertEquals("posted", store.loadAll().first { it.id == "pending" }.status)

        val prefs = source.getSharedPreferences("collector_data", 0)
        val raw = JSONArray(prefs.getString(BillStore.KEY, "[]"))
        raw.getJSONObject(0).put("future_rule", "保留")
        prefs.edit().putString(BillStore.KEY, raw.toString()).commit()
        val edited = store.loadAll().first { it.id == "pending" }.copy(title = "打包盒采购")
        store.upsert(edited)
        val afterEdit = JSONArray(prefs.getString(BillStore.KEY, "[]"))
            .let { array -> (0 until array.length()).map(array::getJSONObject).first { it.getString("id") == "pending" } }
        assertEquals("保留", afterEdit.getString("future_rule"))

        val exported = CompleteBackupStore(source, "entries_v4").exportJson()
        assertEquals(2, JSONObject(exported).getJSONArray("bills").length())
        assertTrue(CompleteBackupStore(target, "entries_v4").importJson(exported))
        assertEquals(2, BillStore(target).loadAll().size)
    }
}
