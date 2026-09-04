package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickEntryFactoryTest {
    @Test fun `名称即可生成兼容默认记录`() {
        val entry = QuickEntryFactory.create("  客厅遥控器  ", 1234L)!!
        assertEquals("客厅遥控器", entry.brand)
        assertEquals("通用", entry.category)
        assertEquals(1, entry.qty)
        assertEquals("件", entry.unit)
        assertEquals("未整理", entry.location)
        assertEquals(1234L, entry.ts)
        assertEquals(1234L, entry.purchaseDate)
    }

    @Test fun `空白名称拒绝保存`() {
        assertNull(QuickEntryFactory.create("   "))
    }

    @Test fun `名称和历史产生可预测默认建议`() {
        assertEquals("食品", QuickEntryFactory.suggest("牛奶", emptyList()).category)
        val history = listOf(Entry(brand = "客厅遥控器", category = "家电", location = "电视柜"))
        val suggestion = QuickEntryFactory.suggest("遥控器", history)
        assertEquals("家电", suggestion.category)
        assertEquals("电视柜", suggestion.location)
    }
}
