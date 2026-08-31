package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebDavConnectionTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun propfindActuallyReachesServerAndRejectsBadCredentials() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var method = ""
        server.createContext("/dav/") { e ->
            method = e.requestMethod
            e.sendResponseHeaders(if (e.requestHeaders.getFirst("Authorization") == "Basic cWE6cWE=") 207 else 401, -1)
            e.close()
        }
        server.start()
        try {
            val store = DesktopDataStore(temp.newFolder())
            store.setWebDavUrl("http://127.0.0.1:${server.address.port}/dav/")
            store.setWebDavUsername("qa"); store.setWebDavPassword("qa")
            assertTrue(DesktopWebDavHelper.testConnection(store).isSuccess)
            assertEquals("PROPFIND", method)
            store.setWebDavPassword("wrong")
            assertFalse(DesktopWebDavHelper.testConnection(store).isSuccess)
        } finally { server.stop(0) }
    }
}
