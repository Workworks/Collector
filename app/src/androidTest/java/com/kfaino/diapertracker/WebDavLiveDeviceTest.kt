package com.kfaino.diapertracker

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.kfaino.collecter.core.BackupDocument
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class WebDavLiveDeviceTest {
    private fun isolated(): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "webdav-qa-${UUID.randomUUID()}-"
        val dir = File(base.cacheDir,prefix).apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix+name, mode)
            override fun getFilesDir() = dir
            override fun getExternalFilesDir(type: String?): File? = null
        }
    }
    @Test fun publicHttpsUploadDownloadAndRestoreAllCollections() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val config = File(ctx.filesDir,"webdav-live-test.json")
        assumeTrue("Live test requires runtime-only config",config.exists())
        val c = JSONObject(config.readText())
        val url=c.getString("url"); val user=c.getString("username"); val pass=c.getString("password")
        val connection = WebDavSyncHelper.testConnection(url,user,pass)
        assertTrue(connection.second,connection.first)
        assertFalse(WebDavSyncHelper.testConnection(url,user,"wrong").first)
        val source = isolated(); val target = isolated()
        val prefs=source.getSharedPreferences("collector_data",0)
        val attachment=File(source.filesDir,"photo.bin").apply {writeBytes(byteArrayOf(1,2,3,4,5))}
        for ((collection,key) in CompleteBackupStore.collectionKeys) {
            prefs.edit().putString(key,JSONArray().put(JSONObject().put("id",collection).put("future","中文保留")).toString()).commit()
        }
        prefs.edit().putString("entries_v4",JSONArray().put(JSONObject().put("id","android-qa").put("img_p",attachment.path)).toString()).commit()
        val exported=CompleteBackupStore(source,"entries_v4").exportJson()
        val upload=WebDavSyncHelper.uploadBackup(url,user,pass,exported)
        assertTrue(upload.second,upload.first)
        val download=WebDavSyncHelper.downloadBackup(url,user,pass)
        assertTrue(download.second,download.first)
        assertEquals(exported,download.third)
        assertTrue(BackupDocument.preview(download.third).contains("entries：1"))
        assertTrue(CompleteBackupStore(target,"entries_v4").importJson(download.third))
        val restored=target.getSharedPreferences("collector_data",0)
        for ((_,key) in CompleteBackupStore.collectionKeys) assertEquals("中文保留",JSONArray(restored.getString(key,"[]")).getJSONObject(0).getString("future"))
        val photo=JSONArray(restored.getString("entries_v4","[]")).getJSONObject(0).getString("img_p")
        assertArrayEquals(attachment.readBytes(),File(photo).readBytes())
    }
}
