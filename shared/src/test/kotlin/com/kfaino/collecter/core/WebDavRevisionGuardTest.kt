package com.kfaino.collecter.core

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class WebDavRevisionGuardTest {
    @Test fun firstUploadObservedRevisionAndStaleWriter() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var status = 404
        var revision = "\"one\""
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("X-Collecter-Backup", "history-etag-v1")
            exchange.responseHeaders.add("ETag", revision)
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}/Collecter_Backup.json"
            assertEquals("If-None-Match" to "*", WebDavRevisionGuard.prepare(url, "qa", "Basic cWE6cWE="))
            status = 200
            assertThrows(IllegalArgumentException::class.java) { WebDavRevisionGuard.prepare(url,"qa","Basic cWE6cWE=") }
            val download = URL(url).openConnection() as HttpURLConnection
            try { download.responseCode; WebDavRevisionGuard.remember(url,"qa",download) }
            finally { download.disconnect() }
            assertEquals("If-Match" to revision, WebDavRevisionGuard.prepare(url,"qa","Basic cWE6cWE="))
            revision = "\"two\""
            assertThrows(IllegalArgumentException::class.java) { WebDavRevisionGuard.prepare(url,"qa","Basic cWE6cWE=") }
            assertThrows(IllegalArgumentException::class.java) { WebDavRevisionGuard.prepare(url,"another-user","Basic cWE6cWE=") }
        } finally { server.stop(0) }
    }
}
