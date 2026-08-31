package com.kfaino.collector.desktop.sync

import com.kfaino.collector.desktop.storage.DesktopDataStore
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 跨平台桌面端原生 WebDAV 云同步引擎
 * 支持坚果云、Nextcloud、群晖 Synology、OwnCloud 等标准 WebDAV 服务器
 */
object DesktopWebDavHelper {

    private const val REMOTE_DIR_NAME = "CollectorBackup"
    private const val REMOTE_FILE_NAME = "collector_cloud_backup.json"

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
        if (!base.endsWith("/")) base += "/"
        return "$base$REMOTE_DIR_NAME/$REMOTE_FILE_NAME"
    }

    private fun getRemoteDirUrl(store: DesktopDataStore): String {
        var base = store.getWebDavUrl().trim()
        if (!base.endsWith("/")) base += "/"
        return "$base$REMOTE_DIR_NAME/"
    }

    fun testConnection(store: DesktopDataStore): SyncResult {
        return try {
            val url = URI.create(store.getWebDavUrl()).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PROPFIND"
            conn.setRequestProperty("Authorization", getAuthHeader(store))
            conn.setRequestProperty("Depth", "0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299 || code == 207) {
                SyncResult(true, "WebDAV 服务器连接成功！(HTTP $code)")
            } else {
                SyncResult(false, "连接失败，服务器返回 HTTP $code。请检查服务器地址、用户名及应用独立密码。")
            }
        } catch (e: Exception) {
            SyncResult(false, "连接异常: ${e.localizedMessage}")
        }
    }

    private fun ensureRemoteDirectoryExists(store: DesktopDataStore) {
        try {
            val dirUrl = URI.create(getRemoteDirUrl(store)).toURL()
            val conn = dirUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "MKCOL"
            conn.setRequestProperty("Authorization", getAuthHeader(store))
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            // Directory might already exist
        }
    }

    fun uploadBackup(store: DesktopDataStore): SyncResult {
        return try {
            if (store.getWebDavUsername().isBlank() || store.getWebDavPassword().isBlank()) {
                return SyncResult(false, "请先在设置中填写 WebDAV 用户名与独立应用密码！")
            }

            ensureRemoteDirectoryExists(store)

            val jsonContent = store.exportJson()
            val fileUrl = URI.create(getFullRemoteFileUrl(store)).toURL()
            val conn = fileUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Authorization", getAuthHeader(store))
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            conn.outputStream.use { os ->
                os.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
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

    fun downloadAndRestore(store: DesktopDataStore): SyncResult {
        return try {
            if (store.getWebDavUsername().isBlank() || store.getWebDavPassword().isBlank()) {
                return SyncResult(false, "请先在设置中填写 WebDAV 用户名与独立应用密码！")
            }

            val fileUrl = URI.create(getFullRemoteFileUrl(store)).toURL()
            val conn = fileUrl.openConnection() as HttpURLConnection
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
                baos.write(buffer, 0, len)
            }
            conn.disconnect()

            val jsonStr = baos.toString(StandardCharsets.UTF_8)
            val success = store.importJson(jsonStr)

            if (success) {
                SyncResult(true, "已成功从 WebDAV 云端恢复数据并即时生效！")
            } else {
                SyncResult(false, "解析云端备份失败，数据格式可能已损坏。")
            }
        } catch (e: Exception) {
            SyncResult(false, "从云端恢复备份失败: ${e.localizedMessage}")
        }
    }
}
