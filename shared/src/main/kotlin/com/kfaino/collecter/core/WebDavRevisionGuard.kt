package com.kfaino.collecter.core

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Session revisions are deliberately not persisted: after restart, inspect the remote backup first. */
object WebDavRevisionGuard {
    private val revisions = ConcurrentHashMap<String, String>()
    private fun key(url: String, user: String) = "$user\n$url"

    fun remember(url: String, user: String, connection: HttpURLConnection) {
        if (connection.getHeaderField("X-Collecter-Backup") == "history-etag-v1") {
            connection.getHeaderField("ETag")?.let { revisions[key(url, user)] = it }
        }
    }

    fun prepare(url: String, user: String, authorization: String): Pair<String, String>? {
        FamilyEndpoint.requireTrustedTransport(URL(url))
        val probe = URL(url).openConnection() as HttpURLConnection
        try {
            probe.requestMethod = "HEAD"
            probe.connectTimeout = 15000
            probe.readTimeout = 20000
            probe.instanceFollowRedirects = false
            probe.setRequestProperty("Authorization", authorization)
            val code = probe.responseCode
            if (probe.getHeaderField("X-Collecter-Backup") != "history-etag-v1") return null
            if (code == 404) return "If-None-Match" to "*"
            require(code in 200..299) { "无法核对远端版本 (HTTP $code)" }
            val observed = revisions[key(url, user)]
            require(observed != null && observed == probe.getHeaderField("ETag")) {
                "远端存在未核对或已改变的备份，请先下载核对再上传；本地数据未改变"
            }
            return "If-Match" to observed
        } finally { probe.disconnect() }
    }
}
