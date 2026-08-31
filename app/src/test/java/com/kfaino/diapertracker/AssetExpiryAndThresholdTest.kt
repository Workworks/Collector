package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * ⏳ 临期判定、低库存预警、维保日历与借还逾期状态单元测试。
 */
class AssetExpiryAndThresholdTest {

    @Test
    fun `耗材低库存安全预警判定准确性`() {
        val lowItem = Entry(
            brand = "抽纸",
            qty = 2,
            minStockThreshold = 5,
            isIn = true,
            isRetired = false
        )
        val normalItem = Entry(
            brand = "洗洁精",
            qty = 10,
            minStockThreshold = 2,
            isIn = true,
            isRetired = false
        )
        val unconfiguredItem = Entry(
            brand = "书本",
            qty = 1,
            minStockThreshold = 0,
            isIn = true,
            isRetired = false
        )

        assertTrue("库存 2 <= 预警阈值 5 应触发预警", lowItem.isLowStock())
        assertFalse("库存 10 > 预警阈值 2 不应触发预警", normalItem.isLowStock())
        assertFalse("未设置预警阈值 (0) 不应触发预警", unconfiguredItem.isLowStock())
    }

    @Test
    fun `耐用资产定期维保周期推算准确性`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        val purchaseTs = cal.timeInMillis

        val waterFilter = Entry(
            brand = "净水器",
            purchaseDate = purchaseTs,
            maintenanceIntervalMonths = 6, // 6 个月维保周期
            lastMaintainedAt = 0L
        )

        assertTrue(waterFilter.isMaintenanceEnabled())
        val nextDate = waterFilter.getNextMaintenanceDate()

        val expectedCal = Calendar.getInstance()
        expectedCal.set(2026, Calendar.JULY, 1, 10, 0, 0)
        // 允许时区毫秒级微小差异，比对年份和月份
        val actualCal = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(2026, actualCal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, actualCal.get(Calendar.MONTH))
    }

    @Test
    fun `物品实物外借逾期判定准确性`() {
        val now = System.currentTimeMillis()
        val overdueEntry = Entry(
            brand = "电钻",
            isLentOut = true,
            currentBorrower = "老李",
            currentLentDate = now - 10L * 24 * 3600 * 1000,
            expectedReturnDate = now - 2L * 24 * 3600 * 1000 // 约定前天归还
        )

        val unexpiredEntry = Entry(
            brand = "帐篷",
            isLentOut = true,
            currentBorrower = "小王",
            currentLentDate = now,
            expectedReturnDate = now + 5L * 24 * 3600 * 1000 // 约定5天后归还
        )

        val notLentEntry = Entry(
            brand = "相机",
            isLentOut = false
        )

        assertTrue("约定归还日早于当前时间应判定逾期", overdueEntry.isLendingOverdue())
        assertFalse("约定归还日在未来不应判定逾期", unexpiredEntry.isLendingOverdue())
        assertFalse("未借出物品不应判定逾期", notLentEntry.isLendingOverdue())
    }

    @Test
    fun `换季衣橱次均穿戴成本精算准确性`() {
        val coat = WardrobeRecord(
            name = "羊绒大衣",
            purchasePrice = 1200.0,
            wearCount = 12
        )
        assertEquals(100.0, coat.getCostPerWear(), 0.001)

        val newCoat = WardrobeRecord(
            name = "新买风衣",
            purchasePrice = 600.0,
            wearCount = 0
        )
        // 0次打卡按初始1次折算基准
        assertEquals(600.0, newCoat.getCostPerWear(), 0.001)
    }

    @Test
    fun `换季衣橱长期沉睡未穿预警判定准确性`() {
        val now = System.currentTimeMillis()
        val sleepingCloth = WardrobeRecord(
            name = "过季衬衫",
            isSealed = false,
            lastWornAt = now - 200L * 24 * 3600 * 1000 // 200天前穿过
        )
        val sealedCloth = WardrobeRecord(
            name = "封存羽绒服",
            isSealed = true, // 封箱状态不触发沉睡预警
            lastWornAt = now - 200L * 24 * 3600 * 1000
        )
        val activeCloth = WardrobeRecord(
            name = "常穿卫衣",
            isSealed = false,
            lastWornAt = now - 10L * 24 * 3600 * 1000
        )

        assertTrue("超过180天未穿且未封箱应触发沉睡预警", sleepingCloth.isSleeping())
        assertFalse("换季封存状态下的衣物不触发沉睡预警", sealedCloth.isSleeping())
        assertFalse("近期穿过的衣物不触发沉睡预警", activeCloth.isSleeping())
    }

    @Test
    fun `家庭应急物资时效与轮换演练判定准确性`() {
        val now = System.currentTimeMillis()
        val expiredBiscuits = EmergencyItem(
            name = "压缩饼干",
            expiryDate = now - 2L * 24 * 3600 * 1000 // 2天前已过期
        )
        val expiringMask = EmergencyItem(
            name = "防毒面具",
            expiryDate = now + 15L * 24 * 3600 * 1000 // 15天后到期
        )
        val durableRadio = EmergencyItem(
            name = "手摇收音机",
            expiryDate = 0L,
            rotationIntervalMonths = 6,
            lastCheckedAt = now - 200L * 24 * 3600 * 1000 // 超过6个月未点检
        )
        val freshWater = EmergencyItem(
            name = "纯净水",
            expiryDate = now + 365L * 24 * 3600 * 1000,
            rotationIntervalMonths = 12,
            lastCheckedAt = now - 30L * 24 * 3600 * 1000
        )

        assertTrue("过期物资应判定为 isExpired", expiredBiscuits.isExpired())
        assertTrue("30天内到期物资应判定为 isExpiringSoon", expiringMask.isExpiringSoon())
        assertFalse("未到期物资不应判定为 isExpired", expiringMask.isExpired())
        assertTrue("超过轮换周期的耐用物资应提示 isNeedsCheck", durableRadio.isNeedsCheck())
        assertFalse("正常有效期且刚点检过的物资不应提示 isNeedsCheck", freshWater.isNeedsCheck())
    }

    @Test
    fun `全维度收纳大厅时效健康度评分算法准确性`() {
        fun calculateHealthScore(expiredCount: Int, expiringCount: Int): Int {
            val penalty = (expiredCount * 6 + expiringCount * 2).coerceAtMost(60)
            return 100 - penalty
        }

        assertEquals(100, calculateHealthScore(0, 0)) // 完美状态
        assertEquals(88, calculateHealthScore(2, 0)) // 2项过期扣12分 -> 88
        assertEquals(86, calculateHealthScore(2, 1)) // 2项过期扣12分 + 1项临期扣2分 -> 86
        assertEquals(40, calculateHealthScore(15, 10)) // 最多扣60分封顶 -> 40分
    }

    @Test
    fun `家庭工具五金维保排期与逾期状态判定准确性`() {
        val now = System.currentTimeMillis()
        val overdueFilter = ToolMaintenanceRecord(
            name = "净水器滤芯",
            maintenanceIntervalDays = 180,
            lastMaintainedAt = now - 200L * 24 * 3600 * 1000 // 200天前更换，已逾期20天
        )
        val expiringFreshFilter = ToolMaintenanceRecord(
            name = "新风初效滤网",
            maintenanceIntervalDays = 90,
            lastMaintainedAt = now - 80L * 24 * 3600 * 1000 // 80天前更换，剩10天需维保
        )
        val normalDrill = ToolMaintenanceRecord(
            name = "冲击钻",
            maintenanceIntervalDays = 0, // 无需周期维保
            lastMaintainedAt = 0L
        )

        assertTrue("超过维保周期的耗材应判定为 isMaintenanceDue", overdueFilter.isMaintenanceDue())
        assertFalse("未到期的耗材不应判定为 isMaintenanceDue", expiringFreshFilter.isMaintenanceDue())
        assertTrue("15天内到期的耗材应判定为 isMaintenanceDueSoon", expiringFreshFilter.isMaintenanceDueSoon())
        assertFalse("无需维保周期的工具不应判定为 isMaintenanceDue", normalDrill.isMaintenanceDue())
    }

    @Test
    fun `家庭绿植花卉水肥日历排期与需浇水施肥判定准确性`() {
        val now = System.currentTimeMillis()
        val thirstyMonstera = PlantCareRecord(
            name = "龟背竹",
            waterIntervalDays = 7,
            lastWateredAt = now - 9L * 24 * 3600 * 1000, // 9天前浇水，已逾期2天
            fertilizeIntervalDays = 30,
            lastFertilizedAt = now - 40L * 24 * 3600 * 1000 // 40天前施肥，已逾期10天
        )
        val freshSucculent = PlantCareRecord(
            name = "姬秋丽多肉",
            waterIntervalDays = 15,
            lastWateredAt = now - 2L * 24 * 3600 * 1000, // 2天前刚浇水，水分充足
            fertilizeIntervalDays = 0, // 无需定期施肥
            lastFertilizedAt = 0L
        )

        assertTrue("超过浇水周期的绿植应判定为 isWaterDue", thirstyMonstera.isWaterDue())
        assertTrue("超过施肥周期的绿植应判定为 isFertilizeDue", thirstyMonstera.isFertilizeDue())
        assertFalse("刚浇水且水分充足的多肉不应判定为 isWaterDue", freshSucculent.isWaterDue())
        assertFalse("未设施肥周期的多肉不应判定为 isFertilizeDue", freshSucculent.isFertilizeDue())
    }

    @Test
    fun `家庭萌宠疫苗驱虫排期与逾期状态判定准确性`() {
        val now = System.currentTimeMillis()
        val overdueDog = PetCareRecord(
            name = "旺财",
            species = "金毛犬",
            dewormIntervalDays = 30,
            lastDewormedAt = now - 35L * 24 * 3600 * 1000, // 35天前驱虫，已逾期5天
            vaccineIntervalDays = 365,
            lastVaccinatedAt = now - 400L * 24 * 3600 * 1000 // 400天前接种疫苗，已逾期35天
        )
        val freshCat = PetCareRecord(
            name = "咪咪",
            species = "英短蓝猫",
            dewormIntervalDays = 30,
            lastDewormedAt = now - 25L * 24 * 3600 * 1000, // 25天前驱虫，剩5天待驱虫 (isDewormDueSoon)
            vaccineIntervalDays = 365,
            lastVaccinatedAt = now - 100L * 24 * 3600 * 1000 // 100天前接种，疫苗有效期充裕
        )

        assertTrue("超过驱虫周期的萌宠应判定为 isDewormDue", overdueDog.isDewormDue())
        assertTrue("超过疫苗周期的萌宠应判定为 isVaccineDue", overdueDog.isVaccineDue())
        assertFalse("未到驱虫日的猫咪不应判定为 isDewormDue", freshCat.isDewormDue())
        assertTrue("7天内需驱虫的猫咪应判定为 isDewormDueSoon", freshCat.isDewormDueSoon())
        assertFalse("疫苗充裕的猫咪不应判定为 isVaccineDue", freshCat.isVaccineDue())
    }

    @Test
    fun `书房藏书阅读进度百分比与借阅归档状态判定准确性`() {
        val readingBook = BookRecord(
            title = "置身事内",
            author = "兰小欢",
            totalPages = 340,
            currentPages = 170, // 刚好 50%
            readingStatus = "reading"
        )
        val lentBook = BookRecord(
            title = "三体",
            author = "刘慈欣",
            borrowerName = "同事小张",
            readingStatus = "lent"
        )
        val finishedBook = BookRecord(
            title = "人类简史",
            totalPages = 400,
            currentPages = 400,
            readingStatus = "finished"
        )

        assertEquals("已读170/340页进度应为50%", 50, readingBook.getProgressPercent())
        assertTrue("readingBook 应判定为 isReading", readingBook.isReading())
        assertFalse("readingBook 不应判定为 isFinished", readingBook.isFinished())
        assertFalse("readingBook 不应判定为 isLent", readingBook.isLent())

        assertTrue("lentBook 应判定为 isLent", lentBook.isLent())
        assertFalse("外借中的书籍不应判定为 isReading", lentBook.isReading())

        assertTrue("finishedBook 应判定为 isFinished", finishedBook.isFinished())
        assertEquals("读完全本进度应为100%", 100, finishedBook.getProgressPercent())
    }

    @Test
    fun `茶窖酒品陈化年份、适饮黄金期与开封保鲜超期判定准确性`() {
        val now = System.currentTimeMillis()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        val maotai = BeverageTeaRecord(
            name = "飞天茅台 53度",
            vintageYear = currentYear - 6, // 6年前酿造
            bestDrinkingYear = currentYear, // 当前正好处于适饮期
            openedAt = 0L // 未开瓶
        )

        val expiredWine = BeverageTeaRecord(
            name = "波尔多干红",
            vintageYear = currentYear - 3,
            bestDrinkingYear = currentYear + 2,
            openedAt = now - 5L * 24 * 3600 * 1000, // 5天前已开瓶
            openShelfLifeDays = 3 // 开瓶保质期仅3天 -> 已超期2天
        )

        val freshCoffee = BeverageTeaRecord(
            name = "耶加雪菲咖啡豆",
            vintageYear = currentYear,
            openedAt = now - 10L * 24 * 3600 * 1000, // 10天前拆封
            openShelfLifeDays = 30 // 30天赏味期 -> 尚余20天
        )

        assertEquals("茅台陈化年数应为6年", 6, maotai.getAgingYears())
        assertTrue("达到最佳适饮年份应判定为 isPeakDrinkingNow", maotai.isPeakDrinkingNow())
        assertFalse("未开瓶茅台不应判定为 isOpened", maotai.isOpened())
        assertFalse("未开瓶茅台不应判定为 isOpenExpired", maotai.isOpenExpired())

        assertTrue("红酒应判定为 isOpened", expiredWine.isOpened())
        assertTrue("开封超过保质期的红酒应判定为 isOpenExpired", expiredWine.isOpenExpired())

        assertTrue("咖啡豆应判定为 isOpened", freshCoffee.isOpened())
        assertFalse("未过赏味期的咖啡豆不应判定为 isOpenExpired", freshCoffee.isOpenExpired())
    }
}







