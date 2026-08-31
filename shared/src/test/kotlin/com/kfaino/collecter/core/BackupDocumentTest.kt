package com.kfaino.collecter.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupDocumentTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun rejectsMalformedRecordsAndMissingAssets() {
        for (text in listOf("{}", "[]", "{\"entries\":null}", "{\"entries\":[1]}",
            "{\"entries\":[{\"id\":\"x\"},{\"id\":\"x\"}]}",
            "{\"entries\":[]} garbage", "{\"schemaVersion\":999,\"entries\":[]}",
            "{\"entries\":[{\"photo\":\"asset:missing\"}]}")) {
            assertThrows("Must reject $text", Exception::class.java) { BackupDocument.parse(text) }
        }
    }

    @Test fun attachmentsRestoreToNewDirectoryWithUnknownFieldsIntact() {
        val source = temp.newFolder("source")
        val target = temp.newFolder("target")
        val photo = File(source, "photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val root = JSONObject("""{"entries":[{"id":"e","future":{"x":42}}],"clippings":[{"id":"c"}]}""")
        root.getJSONArray("entries").getJSONObject(0).put("img_p", photo.absolutePath)
        root.getJSONArray("clippings").getJSONObject(0).put("images", org.json.JSONArray().put(photo.absolutePath))
        val packed = BackupDocument.attachFiles(root, listOf(source))
        assertEquals(1, packed.getJSONObject("assets").length())
        val restored = BackupDocument.restoreFiles(packed, target)
        val entry = restored.getJSONArray("entries").getJSONObject(0)
        assertArrayEquals(photo.readBytes(), File(entry.getString("img_p")).readBytes())
        assertTrue(File(entry.getString("img_p")).canonicalPath.startsWith(target.canonicalPath))
        assertEquals(42, entry.getJSONObject("future").getInt("x"))
        assertEquals(entry.getString("img_p"), restored.getJSONArray("clippings").getJSONObject(0).getJSONArray("images").getString(0))
    }

    @Test fun corruptOrTraversalAssetNeverCreatesDestination() {
        val target = File(temp.root, "not-created")
        for (key in listOf("../escaped", "a".repeat(64))) {
            val root = JSONObject("""{"entries":[]} """)
                .put("assets", JSONObject().put(key, "AQID"))
            assertThrows(Exception::class.java) { BackupDocument.restoreFiles(root, target) }
            assertFalse(target.exists())
        }
    }
}
