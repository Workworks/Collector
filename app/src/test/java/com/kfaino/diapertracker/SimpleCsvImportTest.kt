package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleCsvImportTest {
    @Test fun `CSV 支持中英文表头引号及坏行隔离`() {
        val result = SimpleCsvImport.parse("名称,分类,位置,备注,数量\n纸巾,耗材,储物柜,家庭用,3\n\"相册,一本\",收藏品,,,1\n,无效,,,", 100L)
        assertEquals(2, result.entries.size)
        assertEquals(1, result.rejectedRows)
        assertEquals("相册,一本", result.entries[1].brand)
        assertEquals("未整理", result.entries[1].location)
    }
}
