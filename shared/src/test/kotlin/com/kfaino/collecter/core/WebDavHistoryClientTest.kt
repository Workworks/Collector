package com.kfaino.collecter.core

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.Assert.*
import org.junit.Test

class WebDavHistoryClientTest {
    @Test fun capabilityAuthenticationAndRevisionValidation() {
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        val id="12345678-1234-4123-8123-123456789012"
        server.createContext("/dav/Collecter_Backup.json") {exchange->
            if(exchange.requestHeaders.getFirst("Authorization")!="Basic cWE6cWE=") exchange.sendResponseHeaders(401,-1)
            else {
                exchange.responseHeaders.add("X-Collecter-Backup","history-etag-v1")
                val body=if(exchange.requestURI.query=="history") """[{"id":"$id","reason":"overwrite"}]""" else """{"entries":[{"id":"restored"}]}"""
                val bytes=body.toByteArray();exchange.sendResponseHeaders(200,bytes.size.toLong());exchange.responseBody.use {it.write(bytes)}
            }
            exchange.close()
        }
        server.start()
        try {
            val url="http://127.0.0.1:${server.address.port}/dav/"
            assertEquals(id,WebDavHistoryClient.list(url,"qa","qa").getJSONObject(0).getString("id"))
            assertEquals("restored",BackupDocument.parse(WebDavHistoryClient.download(url,"qa","qa",id)).getJSONArray("entries").getJSONObject(0).getString("id"))
            assertThrows(IllegalArgumentException::class.java){WebDavHistoryClient.list(url,"qa","wrong")}
            assertThrows(IllegalArgumentException::class.java){WebDavHistoryClient.download(url,"qa","qa","../../private")}
        } finally {server.stop(0)}
    }
}
