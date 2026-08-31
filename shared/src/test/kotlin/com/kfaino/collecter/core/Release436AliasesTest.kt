package com.kfaino.collecter.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class Release436AliasesTest {
    @Test fun shortAliasesAndProgressSurviveIdempotentConversion() {
        val input = JSONObject("""{"tools":[{"id":"t","m_days":30,"m_at":123,"future":"kept"}],"clippings":[{"id":"c","cap_at":456,"prog":75}],"beverages":[{"id":"b","qty":0.5,"aging":2030,"p_days":7,"open_at":123}]}""")
        val converted = WireAliases.convert(input)
        assertEquals(30, converted.getJSONArray("tools").getJSONObject(0).getInt("interval_d"))
        assertEquals("kept", converted.getJSONArray("tools").getJSONObject(0).getString("future"))
        assertEquals(0.75, converted.getJSONArray("clippings").getJSONObject(0).getDouble("progress"), 0.0)
        assertEquals(2030, converted.getJSONArray("beverages").getJSONObject(0).getInt("best_year"))
        assertEquals(0.5, converted.getJSONArray("beverages").getJSONObject(0).getDouble("qty"), 0.0)
        assertTrue(converted.similar(WireAliases.convert(converted)))
        assertFalse(input.getJSONArray("tools").getJSONObject(0).has("interval_d"))
    }
}
