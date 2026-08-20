package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 📦 备份编解码器 BackupCodec 单元测试。
 * 验证导出与导入数据一致性与多字段无损往返。
 */
class BackupCodecTest {

    @Test
    fun `备份导出与导入完整往返验证`() {
        val categories = listOf("数码", "日用品", "书籍")
        val entries = listOf(
            Entry(
                id = "item-101",
                category = "数码",
                brand = "iPad mini",
                qty = 1,
                price = 3999.0,
                currentValuation = 2400.0,
                location = "书房桌面",
                houseName = "我的家",
                roomName = "书房",
                isImportant = true,
                isSubscription = false,
                notes = "附带 Apple Pencil"
            ),
            Entry(
                id = "item-102",
                category = "日用品",
                brand = "净水器滤芯",
                qty = 2,
                price = 150.0,
                minStockThreshold = 1,
                maintenanceIntervalMonths = 6,
                isImportant = false
            )
        )

        // 1. 导出 JSON
        val exportedJson = BackupCodec.exportBackupJson(categories, entries)
        assertTrue("导出的 JSON 必须包含 version", exportedJson.contains("\"version\""))
        assertTrue("导出的 JSON 必须包含 categories", exportedJson.contains("\"categories\""))
        assertTrue("导出的 JSON 必须包含 entries", exportedJson.contains("\"entries\""))
        assertTrue("导出的 JSON 必须包含物品品牌", exportedJson.contains("iPad mini"))

        // 2. 导入 JSON 往返比对
        val importedCats = mutableListOf<String>()
        val importedEntries = mutableListOf<Entry>()

        val success = BackupCodec.importBackupJson(
            jsonStr = exportedJson,
            getCategories = { emptyList() },
            saveCategories = { importedCats.addAll(it) },
            saveEntries = { importedEntries.addAll(it) }
        )

        assertTrue("导入应成功解析", success)
        assertEquals(3, importedCats.size)
        assertEquals(2, importedEntries.size)

        val firstItem = importedEntries[0]
        assertEquals("item-101", firstItem.id)
        assertEquals("数码", firstItem.category)
        assertEquals("iPad mini", firstItem.brand)
        assertEquals(3999.0, firstItem.price, 0.01)
        assertEquals(2400.0, firstItem.currentValuation, 0.01)
        assertEquals("书房桌面", firstItem.location)
        assertTrue(firstItem.isImportant)
        assertEquals("附带 Apple Pencil", firstItem.notes)
    }
}
