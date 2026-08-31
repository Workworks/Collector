package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DesktopDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storeDir: File
    private lateinit var store: DesktopDataStore

    @Before
    fun setUp() {
        storeDir = tempFolder.newFolder("collecter_desktop_test")
        store = DesktopDataStore(storeDir)
    }

    @Test
    fun testDataDirectoryResolution() {
        val resolved = DesktopDataDirectory.resolve()
        assertNotNull("Resolved data directory should not be null", resolved)
        assertTrue("Data directory should exist or be created", resolved.exists())
    }

    @Test
    fun testEntriesCrudAndCalculations() {
        val entry = Entry(
            brand = "MacBook Pro 16",
            category = "数码",
            price = 19999.0,
            qty = 1,
            unit = "台",
            location = "书房 · 书桌",
            isDepreciating = true,
            purchaseDate = System.currentTimeMillis() - 100L * 24 * 3600 * 1000
        )
        store.addEntry(entry)

        val loaded = store.loadAll()
        assertEquals(1, loaded.size)
        assertEquals("MacBook Pro 16", loaded[0].brand)
        assertTrue("Days owned should be around 100", loaded[0].getDaysOwned() >= 99)
        assertTrue("Daily cost should be positive", loaded[0].getDailyCost() > 0.0)

        // Update
        val updated = loaded[0].copy(price = 18888.0)
        store.updateEntry(updated)
        assertEquals(18888.0, store.loadAll()[0].price, 0.01)

        // Delete
        store.deleteEntry(updated.id)
        assertEquals(0, store.loadAll().size)
    }

    @Test
    fun testAll12VaultsPersistenceAndRoundTrip() {
        val now = System.currentTimeMillis()

        // 1. 卡券
        store.addOrUpdateVoucher(VoucherRecord(title = "星巴克咖啡券", valueAmount = 35.0, expiryDate = now + 86400000L))
        // 2. 证照
        store.addOrUpdateIdentityDoc(IdentityDocRecord(nameOnDoc = "张三", docType = "id_card", certNumber = "110101199001011234"))
        // 3. 药箱
        store.addOrUpdateMedicine(MedicineRecord(name = "布洛芬缓释胶囊", category = "fever", expiryDate = now + 10L * 86400000L))
        // 4. 食材
        store.addOrUpdateFood(FoodRecord(name = "安格斯牛排", zone = "freezer", qty = 2, expDate = now + 2L * 86400000L))
        // 5. 荣誉
        store.addOrUpdateHonorCredential(HonorCredentialRecord(title = "PMP项目管理专业认证", issuer = "PMI"))
        // 6. 衣橱
        store.addOrUpdateWardrobeRecord(WardrobeRecord(name = "始祖鸟硬壳冲锋衣", season = "winter", color = "黑色"))
        // 7. 应急
        store.addOrUpdateEmergencyItem(EmergencyItem(name = "手摇发电多功能收音机", kitType = "earthquake", location = "玄关"))
        // 8. 工具
        store.addOrUpdateToolRecord(ToolMaintenanceRecord(name = "博世冲击钻", spec = "GSB 600", maintenanceIntervalDays = 180, lastMaintainedAt = now - 200L * 86400000L))
        // 9. 绿植
        store.addOrUpdatePlantRecord(PlantCareRecord(name = "琴叶榕", waterIntervalDays = 5, lastWateredAt = now - 6L * 86400000L))
        // 10. 宠物
        store.addOrUpdatePetRecord(PetCareRecord(name = "布丁", species = "cat", weightKg = 4.5, dewormIntervalDays = 30, lastDewormedAt = now - 35L * 86400000L))
        // 11. 藏书
        store.addOrUpdateBookRecord(BookRecord(title = "置身事内", author = "兰小欢", totalPages = 350, currentPages = 200))
        // 12. 茶窖
        store.addOrUpdateBeverageTeaRecord(BeverageTeaRecord(name = "飞天茅台 53度", vintageYear = 2018, optimalAgingYear = 2028))

        // 重新从磁盘文件实例化全新 DataStore 检验持久化无损性
        val reloadedStore = DesktopDataStore(storeDir)

        assertEquals(1, reloadedStore.getVouchers().size)
        assertEquals("星巴克咖啡券", reloadedStore.getVouchers()[0].title)

        assertEquals(1, reloadedStore.getIdentityDocs().size)
        assertEquals("张三", reloadedStore.getIdentityDocs()[0].nameOnDoc)

        assertEquals(1, reloadedStore.getMedicines().size)
        assertEquals("布洛芬缓释胶囊", reloadedStore.getMedicines()[0].name)

        assertEquals(1, reloadedStore.getFoods().size)
        assertEquals("安格斯牛排", reloadedStore.getFoods()[0].name)

        assertEquals(1, reloadedStore.getHonorCredentials().size)
        assertEquals("PMP项目管理专业认证", reloadedStore.getHonorCredentials()[0].title)

        assertEquals(1, reloadedStore.getWardrobeRecords().size)
        assertEquals("始祖鸟硬壳冲锋衣", reloadedStore.getWardrobeRecords()[0].name)

        assertEquals(1, reloadedStore.getEmergencyItems().size)
        assertEquals("手摇发电多功能收音机", reloadedStore.getEmergencyItems()[0].name)

        assertEquals(1, reloadedStore.getToolRecords().size)
        assertEquals("博世冲击钻", reloadedStore.getToolRecords()[0].name)

        assertEquals(1, reloadedStore.getPlantRecords().size)
        assertEquals("琴叶榕", reloadedStore.getPlantRecords()[0].name)

        assertEquals(1, reloadedStore.getPetRecords().size)
        assertEquals("布丁", reloadedStore.getPetRecords()[0].name)

        assertEquals(1, reloadedStore.getBookRecords().size)
        assertEquals("置身事内", reloadedStore.getBookRecords()[0].title)

        assertEquals(1, reloadedStore.getBeverageTeaRecords().size)
        assertEquals("飞天茅台 53度", reloadedStore.getBeverageTeaRecords()[0].name)
    }

    @Test
    fun testVaultAlertAggregator() {
        val now = System.currentTimeMillis()
        store.addOrUpdateVoucher(VoucherRecord(title = "即将到期优惠券", expiryDate = now + 86400000L))
        store.addOrUpdateMedicine(MedicineRecord(name = "已过期感冒药", expiryDate = now - 86400000L))
        store.addOrUpdatePlantRecord(PlantCareRecord(name = "薄荷", waterIntervalDays = 3, lastWateredAt = now - 5L * 86400000L))

        val alerts = DesktopVaultAlertAggregator.aggregate(store)
        assertTrue("Should produce alert items", alerts.isNotEmpty())
        assertTrue("Expired medicine should be in alerts", alerts.any { it.label.contains("已过期") })
        assertTrue("Expiring voucher should be in alerts", alerts.any { it.label.contains("到期") })
        assertTrue("Thirsty plant should be in alerts", alerts.any { it.label.contains("需浇水") })
    }

    @Test
    fun testCsvExportWithUtf8Bom() {
        store.addEntry(Entry(brand = "机械键盘", category = "数码", price = 499.0, qty = 1))
        val csv = store.exportCsv()
        assertTrue("CSV must start with UTF-8 BOM", csv.startsWith("\uFEFF"))
        assertTrue("CSV contains item brand", csv.contains("机械键盘"))
    }
}
