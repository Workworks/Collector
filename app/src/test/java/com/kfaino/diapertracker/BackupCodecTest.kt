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

    @Test
    fun `备份空数据往返验证`() {
        val categories = listOf("数码")
        val entries = emptyList<Entry>()

        val exportedJson = BackupCodec.exportBackupJson(categories, entries)
        assertTrue("导出 JSON 必须包含 version", exportedJson.contains("\"version\""))

        val importedCats = mutableListOf<String>()
        val importedEntries = mutableListOf<Entry>()
        val success = BackupCodec.importBackupJson(
            jsonStr = exportedJson,
            getCategories = { emptyList() },
            saveCategories = { importedCats.addAll(it) },
            saveEntries = { importedEntries.addAll(it) }
        )
        assertTrue("空数据导入应成功", success)
        assertEquals(0, importedEntries.size)
        assertEquals(1, importedCats.size)
    }

    @Test
    fun `旧版JSON缺少新字段应安全回退默认值`() {
        // 手写不含 min_stock / maintenance_months / is_digital 字段的旧格式 JSON
        val oldJson = """{"version":1,"categories":["数码"],"entries":[{"id":"old-001","cat":"数码","brand":"旧手机","qty":1,"price":1000.0,"is_in":true,"ts":0}]}"""

        val importedEntries = mutableListOf<Entry>()
        val success = BackupCodec.importBackupJson(
            jsonStr = oldJson,
            getCategories = { emptyList() },
            saveCategories = { /* 不关心分类 */ },
            saveEntries = { importedEntries.addAll(it) }
        )
        assertTrue("旧版 JSON 应能成功导入", success)
        assertEquals(1, importedEntries.size)
        val item = importedEntries[0]
        assertEquals("old-001", item.id)
        assertEquals(0, item.minStockThreshold)
        assertEquals(0, item.maintenanceIntervalMonths)
        assertEquals(false, item.isDigital)
    }

    @Test
    fun `畸形JSON导入应安全拒绝且不崩溃`() {
        val importedEntries = mutableListOf<Entry>()
        var saveEntriesCalled = false

        val success = BackupCodec.importBackupJson(
            jsonStr = "not-valid-json",
            getCategories = { emptyList() },
            saveCategories = { /* 不关心 */ },
            saveEntries = {
                saveEntriesCalled = true
                importedEntries.addAll(it)
            }
        )
        assertEquals(false, success)
        assertEquals(false, saveEntriesCalled)
        assertEquals(0, importedEntries.size)
    }

    @Test
    fun `订阅类资产字段往返无损验证`() {
        val categories = listOf("网络订阅")
        val billingDate = 1900000000000L  // 固定时间戳
        val entries = listOf(
            Entry(
                id = "sub-001",
                category = "网络订阅",
                brand = "Netflix",
                isSubscription = true,
                subCycle = "月付",
                subNextBillingDate = billingDate,
                subAutoRenew = true,
                price = 68.0
            )
        )

        val exportedJson = BackupCodec.exportBackupJson(categories, entries)
        val importedEntries = mutableListOf<Entry>()
        BackupCodec.importBackupJson(
            jsonStr = exportedJson,
            getCategories = { emptyList() },
            saveCategories = { /* 不关心 */ },
            saveEntries = { importedEntries.addAll(it) }
        )
        assertEquals(1, importedEntries.size)
        val sub = importedEntries[0]
        assertEquals(true, sub.isSubscription)
        assertEquals("月付", sub.subCycle)
        assertEquals(billingDate, sub.subNextBillingDate)
        assertEquals(true, sub.subAutoRenew)
    }

    @Test
    fun `导出CSV必须以UTF8_BOM开头`() {
        val entries = listOf(
            Entry(
                id = "csv-001",
                category = "数码",
                brand = "测试物品",
                qty = 1,
                price = 100.0
            )
        )
        val csv = ExportManager.generateAssetsCsv(entries)
        assertTrue("CSV 第一个字符必须是 UTF-8 BOM (\\uFEFF)", csv.startsWith("\uFEFF"))
    }
}
