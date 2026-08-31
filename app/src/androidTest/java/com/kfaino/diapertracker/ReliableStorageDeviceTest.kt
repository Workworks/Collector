package com.kfaino.diapertracker

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.kfaino.collecter.core.SnapshotSync
import com.kfaino.collecter.core.WorkspaceRecords
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Real Android SharedPreferences/filesystem; never uses the application's actual preference names. */
class ReliableStorageDeviceTest {
    private fun isolated(): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "qa-${UUID.randomUUID()}-"
        val dir = File(base.cacheDir, prefix).apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
            override fun getFilesDir() = dir
            override fun getExternalFilesDir(type: String?): File? = null
        }
    }

    @Test fun allCollectionsAndAttachmentRestoreWithoutLosingUnknownFields() {
        val source = isolated(); val target = isolated()
        val data = source.getSharedPreferences("collector_data", 0)
        val photo = File(source.filesDir, "original.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        for ((collection, key) in CompleteBackupStore.collectionKeys) {
            data.edit().putString(key, JSONArray().put(JSONObject().put("id", collection).put("future", "保留")).toString()).commit()
        }
        data.edit().putString("entries_v4", JSONArray().put(JSONObject().put("id", "item").put("img_p", photo.path)).toString()).commit()
        val exported = CompleteBackupStore(source, "entries_v4").exportJson()
        assertTrue(CompleteBackupStore(target, "entries_v4").importJson(exported))
        val restored = target.getSharedPreferences("collector_data", 0)
        for ((_, key) in CompleteBackupStore.collectionKeys) assertEquals("保留", JSONArray(restored.getString(key, "[]")).getJSONObject(0).getString("future"))
        val path = JSONArray(restored.getString("entries_v4", "[]")).getJSONObject(0).getString("img_p")
        assertTrue(path.startsWith(target.filesDir.path))
        assertArrayEquals(photo.readBytes(), File(path).readBytes())
        assertFalse(File(target.filesDir, "restore-journal.json").exists())
    }

    @Test fun invalidImportsKeepPreferencesAndDeletionDoesNotResurrect() {
        val context = isolated()
        val prefs = context.getSharedPreferences("collector_data", 0)
        prefs.edit().putString("entries_v4", "[{\"id\":\"keep\",\"brand\":\"旧数据\"}]").putBoolean("private-setting", true).commit()
        val store = CompleteBackupStore(context, "entries_v4")
        val old = prefs.all.toMap()
        for (bad in listOf("broken", "{\"entries\":[3]}", "{\"entries\":[{\"id\":1}]}", "{\"entries\":[{\"id\":\"a\"},{\"id\":\"a\"}]}")) {
            assertFalse(store.importJson(bad)); assertEquals(old, prefs.all)
        }
        val original = store.exportJson()
        JsonCollectionWriter.save(prefs, "entries_v4", JSONArray())
        assertEquals(0, SnapshotSync.merge(store.exportJson(), original).document.getJSONArray("entries").length())
    }

    @Test fun linksOriginalsAndReminderStateSurviveReopening() {
        val context = isolated(); val workspace = CollectionWorkspace(context)
        workspace.addText("原始资料 https://example.invalid/manual")
        val record = workspace.records("inbox").getJSONObject(0)
        val ref = "inbox:${record.getString("id")}"
        workspace.link("entries:item", ref)
        workspace.upsert("reminders", WorkspaceRecords.reminderAction(JSONObject().put("id", "expiry:item").put("cycle", "1"), "done"))
        val reopened = CollectionWorkspace(context)
        assertTrue(reopened.related(ref).contains("entries:item"))
        assertTrue(reopened.records("reminders").getJSONObject(0).getBoolean("done"))
        reopened.unlink(ref, "entries:item")
        assertTrue(reopened.related(ref).isEmpty())
        assertEquals(record.getString("original"), reopened.find(ref)!!.getString("original"))
    }
    @Test fun interruptedRestoreRollsBackTypedPreferencesBeforeUse() {
        val context = isolated()
        val main = context.getSharedPreferences("collector_data", 0)
        main.edit().putString("entries_v4", "[]").putBoolean("private-setting", false).commit()
        val previous = JSONObject()
            .put("entries_v4", JSONObject().put("type", "string").put("value", "[{\"id\":\"old\"}]"))
            .put("private-setting", JSONObject().put("type", "boolean").put("value", true))
            .put("count", JSONObject().put("type", "int").put("value", 3))
        File(context.filesDir, "restore-journal.json").writeText(JSONObject().put("main", previous)
            .put("ledgers", JSONObject()).put("kits", JSONObject()).toString())
        CompleteBackupStore(context, "entries_v4").recoverInterruptedRestore()
        assertEquals("old", JSONArray(main.getString("entries_v4", "[]")).getJSONObject(0).getString("id"))
        assertTrue(main.getBoolean("private-setting", false))
        assertEquals(3, main.getInt("count", 0))
        assertFalse(File(context.filesDir, "restore-journal.json").exists())
    }
}
