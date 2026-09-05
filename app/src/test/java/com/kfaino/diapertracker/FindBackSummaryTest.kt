package com.kfaino.diapertracker

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FindBackSummaryTest {
    @Test fun `资产找回卡聚合位置附件和双向关联`() {
        val document = JSONObject()
            .put("entries", JSONArray().put(JSONObject().put("id", "asset").put("brand", "咖啡机").put("cat", "家电").put("loc", "厨房").put("notes", "滤网每月清理").put("img_p", "item.jpg").put("rec_p", "receipt.jpg")))
            .put("clippings", JSONArray().put(JSONObject().put("id", "manual").put("title", "咖啡机说明书")))
            .put("links", JSONArray().put(JSONObject().put("left", "clippings:manual").put("right", "entries:asset")))
        val result = requireNotNull(FindBackSummary.build(document, "entries:asset"))
        assertEquals("咖啡机", result.title)
        assertEquals("厨房", result.location)
        assertEquals(listOf("实物照片", "发票 / 保修卡"), result.attachments.map { it.label })
        assertEquals("咖啡机说明书", result.related.single().title)
        assertEquals("咖啡机", FindBackSummary.build(document, "clippings:manual")!!.related.single().title)
    }

    @Test fun `缺少位置时显示未整理且无效引用不生成摘要`() {
        val document = JSONObject().put("entries", JSONArray().put(JSONObject().put("id", "a").put("brand", "钥匙")))
            .put("links", JSONArray())
        assertEquals("未整理", FindBackSummary.build(document, "entries:a")!!.location)
        assertNull(FindBackSummary.build(document, "entries:missing"))
    }
}
