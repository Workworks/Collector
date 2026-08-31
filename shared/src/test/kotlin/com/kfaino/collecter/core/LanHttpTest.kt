package com.kfaino.collecter.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class LanHttpTest {
    @Test fun utf8BodyUsesByteLengthAndRejectsTruncation() {
        val body = "{\"entries\":[{\"brand\":\"中文💡\"}]}"
        val bytes = body.toByteArray()
        val input = ByteArrayInputStream(("POST /api/push HTTP/1.1\r\nHost: localhost\r\nContent-Length: ${bytes.size}\r\n\r\n").toByteArray() + bytes)
        val request = LanHttp.readHeaders(input)
        assertEquals(body, LanHttp.readBody(input, request.length))
        assertThrows(IllegalArgumentException::class.java) { LanHttp.readBody(ByteArrayInputStream(bytes), bytes.size + 1) }
    }
    @Test fun authRejectsCrossOriginDnsRebindingAndWrongToken() {
        val headers = mapOf("host" to "192.168.1.2:8080", "authorization" to "Bearer secret")
        fun accepts(extra: Map<String, String>) = LanHttp.authorize(LanHttp.Request("GET", "/", headers + extra, 0), "secret")
        assertTrue(accepts(emptyMap()))
        assertFalse(accepts(mapOf("origin" to "https://evil.example")))
        assertFalse(accepts(mapOf("host" to "evil.example")))
        assertFalse(accepts(mapOf("authorization" to "Bearer wrong")))
        assertFalse(accepts(mapOf("sec-fetch-site" to "cross-site")))
    }
    @Test fun ambiguousAndOversizedRequestsFailBeforeReadingBody() {
        for (headers in listOf("Content-Length: 2\r\nContent-Length: 3", "Transfer-Encoding: chunked", "Content-Length: 67108865", "Content-Length: -1")) {
            assertThrows(IllegalArgumentException::class.java) {
                LanHttp.readHeaders(ByteArrayInputStream("POST / HTTP/1.1\r\n$headers\r\n\r\n".toByteArray()))
            }
        }
    }
}
