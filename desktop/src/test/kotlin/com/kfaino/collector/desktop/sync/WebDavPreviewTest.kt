package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.models.Entry
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebDavPreviewTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun localHttpDownloadPreviewsAndCancellationPreservesBytes() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val json = """{"entries":[{"id":"remote","brand":"远端样例"}]}"""
        server.createContext("/Collecter_Backup.json") { exchange ->
            if (exchange.requestHeaders.getFirst("Authorization") != "Basic cWE6cWE=") {
                exchange.sendResponseHeaders(401, -1)
            } else {
                val bytes = json.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server.start()
        try {
            val dir = temp.newFolder()
            val store = DesktopDataStore(dir)
            store.addEntry(Entry(id = "local", brand = "旧数据"))
            store.setWebDavUrl("http://127.0.0.1:${server.address.port}")
            store.setWebDavUsername("qa"); store.setWebDavPassword("qa")
            val bytes = File(dir, "collector_data.json").readBytes()
            var previewed = false
            val canceled = DesktopWebDavHelper.downloadAndRestore(store) { preview ->
                previewed = preview.contains("entries：1"); false
            }
            assertTrue(previewed)
            assertFalse(canceled.isSuccess)
            assertArrayEquals(bytes, File(dir, "collector_data.json").readBytes())
            assertTrue(DesktopWebDavHelper.downloadAndRestore(store) { true }.isSuccess)
            assertEquals("remote", store.loadAll().single().id)
        } finally { server.stop(0) }
    }
}
