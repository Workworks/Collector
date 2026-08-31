package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.models.Entry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Explicit opt-in: credentials and a pre-created disposable remote directory are runtime-only. */
class WebDavLiveTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun publicHttpsBackupRoundTripWithRealStore() {
        val config = System.getenv("COLLECTOR_WEBDAV_TEST_CONFIG")
        assumeTrue("Live WebDAV is opt-in", !config.isNullOrBlank())
        val c = JSONObject(File(config!!).readText())
        val store = DesktopDataStore(temp.newFolder())
        store.setWebDavUrl(c.getString("url")); store.setWebDavUsername(c.getString("username")); store.setWebDavPassword(c.getString("password"))
        val connected = DesktopWebDavHelper.testConnection(store)
        assertTrue(connected.message, connected.isSuccess)
        store.addEntry(Entry(id="webdav-desktop-qa", brand="公网往返样例"))
        val upload = DesktopWebDavHelper.uploadBackup(store)
        assertTrue(upload.message, upload.isSuccess)
        store.addEntry(Entry(id="local-only", brand="取消恢复必须保留"))
        val before = File(store.dataDir,"collector_data.json").readBytes()
        assertFalse(DesktopWebDavHelper.downloadAndRestore(store) { false }.isSuccess)
        assertArrayEquals(before,File(store.dataDir,"collector_data.json").readBytes())
        val restored = DesktopWebDavHelper.downloadAndRestore(store) { true }
        assertTrue(restored.message, restored.isSuccess)
        assertEquals("webdav-desktop-qa",store.loadAll().single().id)
    }

    @Test fun androidPublicBackupRestoresIntoDesktopWithoutLosingFields() {
        val config = System.getenv("COLLECTOR_WEBDAV_ANDROID_TEST_CONFIG")
        assumeTrue("Android live backup is opt-in", !config.isNullOrBlank())
        val c = JSONObject(File(config!!).readText())
        val store = DesktopDataStore(temp.newFolder())
        store.setWebDavUrl(c.getString("url")); store.setWebDavUsername(c.getString("username")); store.setWebDavPassword(c.getString("password"))
        val restored = DesktopWebDavHelper.downloadAndRestore(store) { true }
        assertTrue(restored.message, restored.isSuccess)
        val exported = com.kfaino.collecter.core.BackupDocument.parse(store.exportJson())
        assertEquals("android-qa",exported.getJSONArray("entries").getJSONObject(0).getString("id"))
        for (key in listOf("houses","vouchers","identity_docs","medicines","foods","honors","wardrobe","emergency","tools","plants","pets","books","beverages","ideas","clippings","inbox","links","reminders")) {
            assertEquals(key,"中文保留",exported.getJSONArray(key).getJSONObject(0).getString("future"))
        }
        val assetId = exported.getJSONArray("entries").getJSONObject(0).getString("img_p").removePrefix("asset:")
        assertArrayEquals(byteArrayOf(1,2,3,4,5),java.util.Base64.getDecoder().decode(exported.getJSONObject("assets").getString(assetId)))
    }
}
