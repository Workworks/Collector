package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebDavRevisionAfterRestoreTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun downloadedRevisionAllowsConditionalUploadAndRedirectIsRejected() {
        val store = DesktopDataStore(temp.newFolder())
        val backup = store.exportJson().toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var condition: String? = null
        var redirect = false
        var leaked = false
        server.createContext("/dav/Collecter_Backup.json") { e ->
            e.responseHeaders.add("X-Collecter-Backup", "history-etag-v1")
            e.responseHeaders.add("ETag", "\"revision-1\"")
            if (redirect && e.requestMethod == "GET") {
                e.responseHeaders.add("Location", "/other"); e.sendResponseHeaders(302, -1)
            } else when (e.requestMethod) {
                "HEAD" -> e.sendResponseHeaders(200, -1)
                "GET" -> { e.sendResponseHeaders(200, backup.size.toLong()); e.responseBody.write(backup) }
                "PUT" -> { condition = e.requestHeaders.getFirst("If-Match"); e.requestBody.readBytes(); e.sendResponseHeaders(204, -1) }
            }
            e.close()
        }
        server.createContext("/other") { e -> leaked = true; e.sendResponseHeaders(200, -1); e.close() }
        server.start()
        try {
            store.setWebDavUrl("http://127.0.0.1:${server.address.port}/dav/")
            store.setWebDavUsername("qa"); store.setWebDavPassword("qa")
            assertTrue(DesktopWebDavHelper.downloadAndRestore(store) { true }.isSuccess)
            assertTrue(DesktopWebDavHelper.uploadBackup(store).isSuccess)
            assertEquals("\"revision-1\"", condition)
            redirect = true
            assertFalse(DesktopWebDavHelper.downloadAndRestore(store) { true }.isSuccess)
            assertFalse(leaked)
        } finally { server.stop(0) }
    }
}
