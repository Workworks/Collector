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
                requestMethod = "PROPFIND"
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

    /** 将本地 JSON 备份包上传同步至 WebDAV */
    fun uploadBackup(serverUrl: String, username: String, password: String, jsonContent: String): Pair<Boolean, String> {
        return try {
            val targetUrl = getTargetUrl(serverUrl)
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Authorization", createAuthHeader(username, password))
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonContent)
                writer.flush()
            }

            val code = conn.responseCode
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

    /** 从 WebDAV 云端下载并恢复 JSON 备份包 */
    fun downloadBackup(serverUrl: String, username: String, password: String): Pair<Boolean, String> {
        return try {
            val targetUrl = getTargetUrl(serverUrl)
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Authorization", createAuthHeader(username, password))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()
                Pair(true, sb.toString())
            } else if (code == 404) {
                conn.disconnect()
                Pair(false, "云端未找到备份文件 ($BACKUP_FILENAME)，请先在原设备上传备份")
            } else {
                conn.disconnect()
                Pair(false, "下载失败 (HTTP $code)")
            }
        } catch (e: Exception) {
            Pair(false, "下载失败: ${e.localizedMessage}")
        }
    }
}
