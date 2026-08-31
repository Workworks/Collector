package com.kfaino.collector.desktop.server

import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.models.Entry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.HttpURLConnection
import java.net.URL

class ServerSecurityTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun authOriginEscapingAndShutdownAreEnforcedOverHttp() {
        val store = DesktopDataStore(temp.newFolder())
        store.addEntry(Entry(brand = "<script>alert(1)</script>"))
        val server = EmbeddedWebServer(store, 0)
        server.start()
        val port = server.boundPort
        fun request(path: String, token: String? = null, method: String = "GET", body: String? = null): Pair<Int, String> {
            val conn = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = method
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) { conn.doOutput = true; conn.outputStream.use { it.write(body.toByteArray()) } }
            return try {
                val status = conn.responseCode
                status to (if (status < 400) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
            } finally { conn.disconnect() }
        }
        try {
            assertEquals(401, request("/api/v1/backup/export").first)
            assertEquals(401, request("/api/v1/backup/import", "wrong", "POST", "{\"entries\":[]}").first)
            assertEquals(200, request("/api/v1/backup/export", server.accessToken).first)
            assertEquals(400, request("/api/v1/backup/import", server.accessToken, "POST", "broken").first)
            assertEquals(1, store.loadAll().size)
            java.net.Socket("127.0.0.1", port).use { socket ->
                socket.soTimeout = 2000
                socket.getOutputStream().write(("GET /api/v1/backup/export HTTP/1.1\r\nHost: localhost:$port\r\nAuthorization: Bearer ${server.accessToken}\r\nOrigin: https://evil.example\r\nConnection: close\r\n\r\n").toByteArray())
                val status = socket.getInputStream().bufferedReader().readLine()
                assertTrue(status, status.contains("403"))
            }
            val html = request("/").second
            assertTrue(html.contains("&lt;script&gt;"))
            assertFalse(html.contains("<script>alert"))
            assertEquals(404, request("/api/v1/backup/export/extra", server.accessToken).first)
        } finally { server.stop() }
        assertThrows(Exception::class.java) { request("/api/v1/health") }
    }
}
