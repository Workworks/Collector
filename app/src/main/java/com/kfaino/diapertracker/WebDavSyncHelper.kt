package com.kfaino.diapertracker

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * WebDAV 私有云安全同步引擎 (坚果云 / Nextcloud / 群晖 Synology)
 * - 原生 HttpURLConnection 实现，零第三方臃肿网络库
 * - 支持标准 Basic Auth 鉴权
 * - 支持一键云端上传与云端数据一键还原
 */
object WebDavSyncHelper {

    private const val BACKUP_FILENAME = "Collecter_Backup.json"

    private fun getTargetUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        com.kfaino.collecter.core.FamilyEndpoint.requireTrustedTransport(URL(trimmed))
        return if (trimmed.endsWith(BACKUP_FILENAME)) trimmed else "$trimmed/$BACKUP_FILENAME"
    }

    private fun createAuthHeader(user: String, pass: String): String {
        val credentials = "$user:$pass"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /** 测试 WebDAV 连接状态 */
    fun testConnection(serverUrl: String, username: String, password: String): Pair<Boolean, String> {
        return try {
            val targetUrl = getTargetUrl(serverUrl)
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                // Android HttpURLConnection 不接受 PROPFIND；HEAD 可验证认证与目标可达性。
                requestMethod = "HEAD"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", createAuthHeader(username, password))
                setRequestProperty("Depth", "0")
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299 || code == 404 || code == 207) {
                Pair(true, "连接成功 (状态码: $code)")
            } else if (code == 401 || code == 403) {
                Pair(false, "鉴权失败 (状态码: $code)，请检查用户名和应用独立密码")
            } else {
                Pair(false, "服务器返回异常状态码: $code")
            }
        } catch (e: Exception) {
            Pair(false, "连接失败: ${e.localizedMessage}")
        }
    }

    /**
     * 将本地 JSON 备份包上传同步至 WebDAV（含 1 次自动重试）
     * @return Pair<成功与否, 提示消息>
     */
    fun uploadBackup(serverUrl: String, username: String, password: String, jsonContent: String): Pair<Boolean, String> {
        return uploadOnce(serverUrl, username, password, jsonContent).let { result ->
            if (!result.first) {
                // 等待 1500ms 后重试一次
                try { Thread.sleep(1500) } catch (ie: InterruptedException) { Thread.currentThread().interrupt() }
                val retry = uploadOnce(serverUrl, username, password, jsonContent)
                if (!retry.first) Pair(false, "${retry.second}（已重试 1 次）") else retry
            } else {
                result
            }
        }
    }

    private fun uploadOnce(serverUrl: String, username: String, password: String, jsonContent: String): Pair<Boolean, String> {
        return try {
            val targetUrl = getTargetUrl(serverUrl)
            val condition = com.kfaino.collecter.core.WebDavRevisionGuard.prepare(targetUrl, username, createAuthHeader(username, password))
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Authorization", createAuthHeader(username, password))
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                condition?.let { setRequestProperty(it.first, it.second) }
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonContent)
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) com.kfaino.collecter.core.WebDavRevisionGuard.remember(targetUrl, username, conn)
            conn.disconnect()

            if (code in 200..299 || code == 201 || code == 204) {
                Pair(true, "云端同步成功！已安全保存至 $BACKUP_FILENAME")
            } else {
                Pair(false, "上传失败 (HTTP $code)")
            }
        } catch (e: Exception) {
            Pair(false, "上传失败: ${e.localizedMessage}")
        }
    }

    /**
     * 从 WebDAV 云端下载 JSON 备份包
     * @return Triple<成功与否, 提示消息, JSON内容字符串（失败时为空串）>
     */
    fun downloadBackup(serverUrl: String, username: String, password: String): Triple<Boolean, String, String> {
        return try {
            val targetUrl = getTargetUrl(serverUrl)
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Authorization", createAuthHeader(username, password))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val bytes = conn.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(output.size().toLong() + count <= com.kfaino.collecter.core.BackupDocument.MAX_BYTES) { "备份超过大小限制" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                com.kfaino.collecter.core.WebDavRevisionGuard.remember(targetUrl, username, conn)
                conn.disconnect()
                Triple(true, "云端备份下载成功", String(bytes, Charsets.UTF_8))
            } else if (code == 404) {
                conn.disconnect()
                Triple(false, "云端未找到备份文件 ($BACKUP_FILENAME)，请先在原设备上传备份", "")
            } else {
                conn.disconnect()
                Triple(false, "下载失败 (HTTP $code)", "")
            }
        } catch (e: Exception) {
            Triple(false, "下载失败: ${e.localizedMessage}", "")
        }
    }
}
