package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DesktopSyncMergeEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: DesktopDataStore

    @Before
    fun setUp() {
        val testDir = tempFolder.newFolder("collecter_merge_test")
        store = DesktopDataStore(testDir)
    }

    @Test
    fun testEntryMergeWithTimestampConflictResolution() {
        val localEntry = Entry(
            id = "entry-001",
            brand = "机械键盘",
            price = 500.0,
            ts = 1000L
        )
        store.addEntry(localEntry)

        // 对端传来的同一条目，但修改时间戳更新（2000L），价格改为 450.0
        val incomingJson = JSONObject().apply {
            val arr = JSONArray()
            arr.put(JSONObject().apply {
                put("id", "entry-001")
                put("brand", "机械键盘 (无线版)")
                put("price", 450.0)
                put("ts", 2000L)
            })
            // 同时带来一条全新条目 entry-002
            arr.put(JSONObject().apply {
                put("id", "entry-002")
                put("brand", "人体工学鼠标")
                put("price", 299.0)
                put("ts", 1500L)
            })
            put("entries", arr)
        }.toString()

        val report = DesktopSyncMergeEngine.merge(store, incomingJson)
        assertTrue(report.success)
        assertEquals(1, report.insertedEntries)
        assertEquals(1, report.updatedEntries)

        val all = store.loadAll()
        assertEquals(2, all.size)
        val e1 = all.find { it.id == "entry-001" }!!
        assertEquals("机械键盘 (无线版)", e1.brand)
        assertEquals(450.0, e1.price, 0.01)

        val e2 = all.find { it.id == "entry-002" }!!
        assertEquals("人体工学鼠标", e2.brand)
    }

    @Test
    fun testVaultsIncrementMerge() {
        store.addOrUpdateVoucher(VoucherRecord(id = "v-1", title = "本土洗车券"))

        val incomingJson = JSONObject().apply {
            val vArr = JSONArray().apply {
                put(JSONObject().put("id", "v-2").put("title", "外来咖啡券").put("val", 30.0))
            }
            val mArr = JSONArray().apply {
                put(JSONObject().put("id", "m-1").put("name", "维生素C").put("exp", 2000000000000L))
            }
            put("vouchers", vArr)
            put("medicines", mArr)
        }.toString()

        val report = DesktopSyncMergeEngine.merge(store, incomingJson)
        assertTrue(report.success)
        assertEquals(2, report.mergedVaultItems)

        assertEquals(2, store.getVouchers().size)
        assertEquals(1, store.getMedicines().size)
        assertEquals("维生素C", store.getMedicines()[0].name)
    }
}
