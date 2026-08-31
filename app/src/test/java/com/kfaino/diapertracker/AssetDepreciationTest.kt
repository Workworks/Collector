package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 💰 资产折旧、拥有天数与闲置变现统计单元测试。
 */
class AssetDepreciationTest {

    @Test
    fun `闲置变现与回血 ROI 计算准确性验证`() {
        val now = System.currentTimeMillis()
        val entries = listOf(
            Entry(
                id = "1",
                brand = "iPhone 13",
                price = 5999.0,
                qty = 1,
                isRetired = true,
                retiredSoldPrice = 2800.0,
                retiredAt = now
            ),
            Entry(
                id = "2",
                brand = "Switch 游戏机",
                price = 2099.0,
                qty = 1,
                isRetired = true,
                retiredSoldPrice = 1200.0,
                retiredAt = now
            ),
            Entry(
                id = "3",
                brand = "旧键盘",
                price = 300.0,
                qty = 1,
                isRetired = true,
                retiredSoldPrice = 0.0, // 报废/送人，未回血
                retiredAt = now
            ),
            Entry(
                id = "4",
                brand = "MacBook Pro",
                price = 14999.0,
                qty = 1,
                isRetired = false // 仍是在役资产
            )
        )

        val analytics = AnalyticsQueries.getResaleAnalytics(entries)

        // 仅统计已退役资产: 5999 + 2099 + 300 = 8398.0
        assertEquals(8398.0, analytics.totalInvested, 0.01)
        // 回血总额: 2800 + 1200 = 4000.0
        assertEquals(4000.0, analytics.totalRecovered, 0.01)
        // 净支出成本: 8398 - 4000 = 4398.0
        assertEquals(4398.0, analytics.netCost, 0.01)
        // 回血率: 4000 / 8398 = 47.63%
        assertEquals(47.63, analytics.recoveryRate, 0.05)
        assertEquals(2, analytics.soldItems.size)
    }

    @Test
    fun `在役与退役多态过滤准确性验证`() {
        val entries = listOf(
            Entry(id = "1", brand = "吉他", isRetired = false, isSubscription = false),
            Entry(id = "2", brand = "iCloud", isRetired = false, isSubscription = true),
            Entry(id = "3", brand = "旧音箱", isRetired = true, isSubscription = false)
        )

        val active = entries.filter { !it.isRetired }
        val subs = entries.filter { it.isSubscription }
        val nonSubs = entries.filter { !it.isSubscription }

        assertEquals(2, active.size)
        assertEquals(1, subs.size)
        assertEquals(2, nonSubs.size)
    }
}
