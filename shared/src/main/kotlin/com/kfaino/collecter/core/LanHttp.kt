package com.kfaino.collecter.core

import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Small bounded HTTP/1.1 reader for Android's existing socket transport. Content-Length is bytes. */
object LanHttp {
    data class Request(val method: String, val path: String, val headers: Map<String, String>, val length: Int)
    fun token(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })

    fun readHeaders(input: InputStream): Request {
        var total = 0
        fun line(): String {
            val bytes = ByteArrayOutputStream()
            while (true) {
                val ch = input.read()
                require(ch >= 0) { "请求头不完整" }
                total++
                require(total <= 32768 && bytes.size() <= 8192) { "请求头过大" }
                if (ch == 10) return bytes.toString("US-ASCII").trimEnd('\r')
                bytes.write(ch)
            }
        }
        val start = line().split(' ')
        require(start.size == 3 && start[2] == "HTTP/1.1") { "请求行错误" }
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = line()
            if (line.isEmpty()) break
            require(line.contains(':')) { "请求头错误" }
            val key = line.substringBefore(':').lowercase()
            require(!headers.containsKey(key)) { "重复请求头" }
            headers[key] = line.substringAfter(':').trim()
        }
        require(!headers.containsKey("transfer-encoding")) { "不支持流式编码，请发送明确长度" }
        val length = headers["content-length"]?.toLong() ?: 0L
        require(length in 0..BackupDocument.MAX_BYTES.toLong()) { "请求过大" }
        return Request(start[0], start[1].substringBefore('?'), headers, length.toInt())
    }

    fun readBody(input: InputStream, length: Int): String {
        require(length in 0..BackupDocument.MAX_BYTES)
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(bytes, offset, length - offset)
            require(count > 0) { "请求正文不完整" }
            offset += count
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun authorize(request: Request, token: String): Boolean {
        val host = request.headers["host"] ?: return false
        val name = host.substringBefore(':')
        if (name !in setOf("localhost", "127.0.0.1") && !name.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) return false
        val origin = request.headers["origin"]
        if (origin != null && origin != "http://$host") return false
        if (request.headers["sec-fetch-site"] == "cross-site") return false
        val auth = request.headers["authorization"] ?: return false
        val supplied = if (auth.startsWith("Bearer ")) auth.removePrefix("Bearer ")
        else if (auth.startsWith("Basic ")) try {
            String(Base64.getDecoder().decode(auth.removePrefix("Basic ")), Charsets.UTF_8).substringAfter(':', "")
        } catch (e: IllegalArgumentException) { return false }
        else return false
        return MessageDigest.isEqual(supplied.toByteArray(), token.toByteArray())
    }

    const val UNAUTHORIZED = "HTTP/1.1 401 Unauthorized\r\nWWW-Authenticate: Basic realm=\"Collecter paired device\"\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
}
