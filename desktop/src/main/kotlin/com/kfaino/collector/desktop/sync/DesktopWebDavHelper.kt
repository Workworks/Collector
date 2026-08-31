package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 跨平台桌面端原生 WebDAV 云同步引擎
 * 支持坚果云、Nextcloud、群晖 Synology、OwnCloud 等标准 WebDAV 服务器
 */
object DesktopWebDavHelper {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()

    data class SyncResult(
        val isSuccess: Boolean,
        val message: String
    )

    private fun getAuthHeader(store: DesktopDataStore): String {
        val raw = "${store.getWebDavUsername()}:${store.getWebDavPassword()}"
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }

    private fun getFullRemoteFileUrl(store: DesktopDataStore): String {
        var base = store.getWebDavUrl().trim()
        com.kfaino.collecter.core.FamilyEndpoint.requireTrustedTransport(java.net.URL(base))
        if (!base.endsWith("/")) base += "/"
        return if (base.trimEnd('/').endsWith("Collecter_Backup.json")) base.trimEnd('/') else "${base}Collecter_Backup.json"
    }

    fun testConnection(store: DesktopDataStore): SyncResult {
        return try {
            com.kfaino.collecter.core.FamilyEndpoint.requireTrustedTransport(java.net.URL(store.getWebDavUrl().trim()))
            val request = HttpRequest.newBuilder(URI.create(store.getWebDavUrl().trim()))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", getAuthHeader(store))
                .header("Depth", "0")
                .method("PROPFIND", HttpRequest.BodyPublishers.noBody()).build()
            val code = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()

            if (code in 200..299 || code == 207) {
                SyncResult(true, "WebDAV 服务器连接成功！(HTTP $code)")
            } else {
                SyncResult(false, "连接失败，服务器返回 HTTP $code。请检查服务器地址、用户名及应用独立密码。")
            }
        } catch (e: Exception) {
            SyncResult(false, "连接异常: ${e.localizedMessage}")
        }
    }

    fun uploadBackup(store: DesktopDataStore): SyncResult {
        return try {
            if (store.getWebDavUsername().isBlank() || store.getWebDavPassword().isBlank()) {
                return SyncResult(false, "请先在设置中填写 WebDAV 用户名与独立应用密码！")
            }

            val jsonContent = store.exportJson()
            val fileUrl = URI.create(getFullRemoteFileUrl(store)).toURL()
            val condition = com.kfaino.collecter.core.WebDavRevisionGuard.prepare(fileUrl.toString(), store.getWebDavUsername(), getAuthHeader(store))
            val conn = fileUrl.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Authorization", getAuthHeader(store))
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            condition?.let { conn.setRequestProperty(it.first, it.second) }
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            conn.outputStream.use { os ->
                os.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) com.kfaino.collecter.core.WebDavRevisionGuard.remember(fileUrl.toString(), store.getWebDavUsername(), conn)
            conn.disconnect()

            if (code in 200..299 || code == 201 || code == 204) {
                SyncResult(true, "已成功将数据同步至 WebDAV 私有云！")
            } else {
                SyncResult(false, "云备份上传失败，HTTP 状态码: $code")
            }
        } catch (e: Exception) {
            SyncResult(false, "上传云备份失败: ${e.localizedMessage}")
        }
    }

    fun downloadAndRestore(store: DesktopDataStore, confirm: (String) -> Boolean = { false }): SyncResult {
        return try {
            if (store.getWebDavUsername().isBlank() || store.getWebDavPassword().isBlank()) {
                return SyncResult(false, "请先在设置中填写 WebDAV 用户名与独立应用密码！")
            }

            val fileUrl = URI.create(getFullRemoteFileUrl(store)).toURL()
            val conn = fileUrl.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", getAuthHeader(store))
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return SyncResult(false, "云端未找到备份文件，请确认是否已在移动端或其他设备执行过上传 (HTTP $code)")
            }

            val stream = conn.inputStream
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var len: Int
            while (stream.read(buffer).also { len = it } != -1) {
                require(baos.size().toLong() + len <= com.kfaino.collecter.core.BackupDocument.MAX_BYTES) { "备份超过大小限制" }
                baos.write(buffer, 0, len)
            }
            stream.close()
            com.kfaino.collecter.core.WebDavRevisionGuard.remember(fileUrl.toString(), store.getWebDavUsername(), conn)
            conn.disconnect()

            val jsonStr = baos.toString(StandardCharsets.UTF_8)
            val preview = com.kfaino.collecter.core.BackupDocument.preview(jsonStr)
            if (!confirm(preview)) return SyncResult(false, "已取消恢复，原数据未改变")
            val success = store.importJson(jsonStr)

            if (success) {
                com.kfaino.collecter.core.WebDavRevisionGuard.remember(fileUrl.toString(), store.getWebDavUsername(), conn)
                SyncResult(true, "已成功从 WebDAV 云端恢复数据并即时生效！")
            } else {
                SyncResult(false, "解析云端备份失败，数据格式可能已损坏。")
            }
        } catch (e: Exception) {
            SyncResult(false, "从云端恢复备份失败: ${e.localizedMessage}")
        }
    }
}
