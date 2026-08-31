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
}
