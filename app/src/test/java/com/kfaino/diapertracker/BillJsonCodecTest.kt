package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class BillJsonCodecTest {
    @Test
    fun `bill json preserves integer money and source text`() {
        val original = listOf(
            BillRecord(
                id = "bill-1",
                title = "汽车充电",
                amountMinor = 1442,
                currency = "CNY",
                category = "交通",
                status = "pending",
                note = "0.97kWh +98KM",
                sourceText = "汽车充电: 峰值0.97kw/h 14.42元 +98KM",
                occurredAt = 1_790_352_000_000L,
                createdAt = 1L,
                updatedAt = 2L
            )
        )

        val decoded = BillJsonCodec.decode(BillJsonCodec.encode(original))
        assertEquals(original, decoded)
        assertEquals("¥14.42", BillAmounts.format(decoded.single().amountMinor, "CNY"))
    }

    @Test
    fun `legacy record safely defaults optional fields`() {
        val decoded = BillJsonCodec.decode("""[{"id":"old","title":"旧账单","amount_minor":99}]""").single()
        assertEquals("CNY", decoded.currency)
        assertEquals("expense", decoded.direction)
        assertEquals("posted", decoded.status)
        assertEquals("none", decoded.recurrence)
    }

    @Test
    fun `invalid or duplicate records are rejected`() {
        assertTrue(runCatching { BillJsonCodec.decode("""[{"id":"bad","title":"","amount_minor":100}]""") }.isFailure)
        assertTrue(runCatching { BillJsonCodec.decode("""[{"id":"same","title":"A","amount_minor":100},{"id":"same","title":"B","amount_minor":200}]""") }.isFailure)
        assertEquals(1L, BillAmounts.parseMinor("0.01"))
        assertEquals(null, BillAmounts.parseMinor("0"))
    }

    @Test
    fun `editing known fields preserves future fields`() {
        val original = JSONObject("""{"id":"future","title":"旧标题","amount_minor":100,"future_rule":"保留"}""")
        val updated = BillRecord.fromJson(original).copy(title = "新标题", amountMinor = 200)
        val merged = BillJsonCodec.mergeKnownFields(original, updated)
        assertEquals("新标题", merged.getString("title"))
        assertEquals(200L, merged.getLong("amount_minor"))
        assertEquals("保留", merged.getString("future_rule"))
    }
}
