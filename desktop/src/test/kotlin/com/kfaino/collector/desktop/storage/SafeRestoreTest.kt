package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.Entry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafeRestoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun badImportLeavesExistingFileUnchanged() {
        val dir = temp.newFolder()
        val store = DesktopDataStore(dir)
        store.addEntry(Entry(id = "kept", brand = "Important asset"))
        val file = File(dir, "collector_data.json")
        val original = file.readBytes()
        for (input in listOf("broken", "{}", "{\"entries\":{}}", "{\"entries\":[{\"id\":\"x\"},{\"id\":\"x\"}]}")) {
            assertFalse(store.importJson(input))
            assertArrayEquals(original, file.readBytes())
            assertEquals("kept", store.loadAll().single().id)
        }
    }

    @Test fun legacyImportRetainsAbsentCollectionsAndNewFieldsSurviveEdit() {
        val store = DesktopDataStore(temp.newFolder())
        assertTrue(store.importJson("""{"entries":[{"id":"e","brand":"before","future":42}],"ideas":[{"id":"idea","title":"keep"}]}"""))
        store.updateEntry(store.loadAll().single().copy(brand = "after"))
        var root = JSONObject(store.exportJson())
        assertEquals(42, root.getJSONArray("entries").getJSONObject(0).getInt("future"))
        assertTrue(store.importJson("""{"entries":[]} """))
        root = JSONObject(store.exportJson())
        assertEquals("idea", root.getJSONArray("ideas").getJSONObject(0).getString("id"))
    }
    @Test fun androidYearlyCycleSurvivesDesktopEdit() {
        val store = DesktopDataStore(temp.newFolder())
        assertTrue(store.importJson("""{"entries":[{"id":"subscription","brand":"年度服务","sub_cyc":"按年"}]}"""))
        assertEquals("YEARLY", store.loadAll().single().subCycle.name)
        store.updateEntry(store.loadAll().single().copy(brand = "修改名称"))
        assertEquals("按年", JSONObject(store.exportJson()).getJSONArray("entries").getJSONObject(0).getString("sub_cyc"))
    }
}
