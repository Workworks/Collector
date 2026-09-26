package com.kfaino.collecter.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillQuickInputParserTest {
    private val today = LocalDate.of(2026, 9, 26)

    @Test
    fun `recommended format parses dates currencies states and recurrence`() {
        val result = BillQuickInputParser.parse(
            """
            @2026-09-25
            支出 52 餐饮 小母鸡 #已入账 ; 原价45+配送7
            支出 20USD 订阅 ChatGPT #每月 #未入账 ; 下月停
            收入 200 退款 采购退款 #已入账
            """.trimIndent(), today
        )

        assertTrue(result.issues.isEmpty())
        assertEquals(3, result.records.size)
        assertEquals(5200L, result.records[0].amountMinor)
        assertEquals(LocalDate.of(2026, 9, 25), result.records[0].occurredOn)
        assertEquals("USD", result.records[1].currency)
        assertEquals("pending", result.records[1].status)
        assertEquals("monthly", result.records[1].recurrence)
        assertEquals("income", result.records[2].direction)
    }

    @Test
    fun `handwritten note syntax from screenshot remains usable`() {
        val result = BillQuickInputParser.parse(
            """
            采购价格汇总列表:
            chatgpt: 20USD
            claude: 20USD（耗资，下个月停）

            9/25晚
            采购+40（未入账）

            记账9/26
            小母鸡: 52->45 +7元
            白菜: 网购
            猪肉: -9.9（10.8/斤）
            配送: 世贸+8（未入账）
            打包盒采购: +28.82（未入账）
            汽车加油: 5.84L 50元，+120KM
            汽车充电: 峰值0.97kw/h 14.42元 +98KM
            """.trimIndent(), today
        )

        assertEquals(9, result.records.size)
        assertEquals(1, result.issues.size)
        assertEquals("白菜: 网购", result.issues.single().sourceText)
        assertEquals("USD", result.records[0].currency)
        assertEquals("monthly", result.records[1].recurrence)
        assertEquals(LocalDate.of(2026, 9, 25), result.records[2].occurredOn)
        assertEquals("pending", result.records[2].status)
        assertEquals(5200L, result.records[3].amountMinor)
        assertEquals(990L, result.records[4].amountMinor)
        assertEquals(800L, result.records[5].amountMinor)
        assertEquals(2882L, result.records[6].amountMinor)
        assertEquals(5000L, result.records[7].amountMinor)
        assertEquals(1442L, result.records[8].amountMinor)
        assertTrue(result.records[8].note.contains("98KM"))
    }

    @Test
    fun `unknown and unsafe amounts never become records`() {
        val result = BillQuickInputParser.parse(
            """
            白菜：网购
            支出 0 餐饮 空金额
            支出 999999999 CNY 其他 过大金额
            """.trimIndent(), today
        )

        assertTrue(result.records.isEmpty())
        assertEquals(3, result.issues.size)
    }
}
