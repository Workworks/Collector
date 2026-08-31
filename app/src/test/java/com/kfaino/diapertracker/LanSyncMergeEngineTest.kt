package com.kfaino.diapertracker

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * 局域网 P2P 增量对撞合并核心引擎单元测试
 */
class LanSyncMergeEngineTest {

    @Test
    fun `合并新数据正常增量插入`() {
        val currentCats = listOf("数码", "日用品")
        val currentEntries = listOf(
            Entry(id = "local-1", brand = "iPhone 15", category = "数码", price = 5999.0, ts = 1000L)
        )

        val incomingJson = JSONObject().apply {
            put("categories", JSONArray().put("食品").put("数码"))
            put("entries", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "remote-1")
                    put("brand", "特级橄榄油")
                    put("category", "食品")
                    put("price", 88.0)
                    put("ts", 1500L)
                })
            })
        }.toString()

        val (report, mergedCats, mergedEntries) = LanSyncMergeEngine.mergeEntriesAndCategories(
            currentCats,
            currentEntries,
            incomingJson
        )

        assertTrue(report.success)
        assertEquals(1, report.insertedEntries)
        assertEquals(0, report.updatedEntries)
        assertEquals(1, report.mergedCategories)

        assertTrue(mergedCats.contains("食品"))
        assertTrue(mergedCats.contains("数码"))
        assertEquals(2, mergedEntries.size)
        assertTrue(mergedEntries.any { it.id == "remote-1" })
        assertTrue(mergedEntries.any { it.id == "local-1" })
    }

    @Test
    fun `同ID冲突条目按时间戳LastWriteWins仲裁`() {
        val currentCats = listOf("数码")
        val currentEntries = listOf(
            Entry(id = "item-dup", brand = "索尼降噪耳机 (旧版)", price = 1200.0, ts = 1000L)
        )

        // 对端条目 ID 相同，时间戳更新 (2000L)，价格更新为 1050.0
        val incomingJson = JSONObject().apply {
            put("entries", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "item-dup")
                    put("brand", "索尼降噪耳机 (已升级蓝牙5.3)")
                    put("price", 1050.0)
                    put("ts", 2000L)
                })
            })
        }.toString()

        val (report, _, mergedEntries) = LanSyncMergeEngine.mergeEntriesAndCategories(
            currentCats,
            currentEntries,
            incomingJson
        )

        assertTrue(report.success)
        assertEquals(0, report.insertedEntries)
        assertEquals(1, report.updatedEntries)
        assertEquals(1, mergedEntries.size)
        assertEquals("索尼降噪耳机 (已升级蓝牙5.3)", mergedEntries[0].brand)
        assertEquals(1050.0, mergedEntries[0].price, 0.01)
    }

    @Test
    fun `空输入与畸形输入安全回退`() {
        val currentCats = listOf("数码")
        val currentEntries = listOf(Entry(id = "item-1"))

        val (emptyReport, _, _) = LanSyncMergeEngine.mergeEntriesAndCategories(currentCats, currentEntries, "")
        assertFalse(emptyReport.success)

        val (malformedReport, _, _) = LanSyncMergeEngine.mergeEntriesAndCategories(currentCats, currentEntries, "{malformed_json")
        assertFalse(malformedReport.success)
    }
}
