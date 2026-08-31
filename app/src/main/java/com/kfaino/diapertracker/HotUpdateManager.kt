package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * 类游戏极速热更新下载、校验与版本管理引擎
 * - 内置多镜像极速 CDN 备用通道（ghfast.top、mirror.ghproxy.com、ghproxy.net）与直连容灾
 * - 智能跟随 301/302 重定向，解决国内网络直连 GitHub Releases 443 超时问题
 */
object HotUpdateManager {

    data class HotPatchInfo(
        val patchVersion: String,
        val targetBaseVersion: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val md5: String,
        val changelog: String,
        val title: String
    )

    private val executor = Executors.newSingleThreadExecutor()

    fun checkHotPatch(context: Context, onResult: (HotPatchInfo?, String?) -> Unit) {
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        val baseVer = UpdateManager.getAppVersionName(context)
        val activePatchVer = HotPatchEngine.getActivePatchVersion(context)
        val store = DataStore(context)
        val repo = store.getGithubRepo().ifBlank { "Workworks/Collector" }
        val apiUrls = listOf(
            "https://ghfast.top/https://api.github.com/repos/$repo/releases/latest",
            "https://mirror.ghproxy.com/https://api.github.com/repos/$repo/releases/latest",
            "https://ghproxy.net/https://api.github.com/repos/$repo/releases/latest",
            "https://api.github.com/repos/$repo/releases/latest"
        )

        executor.execute {
            var lastError = "无法连接至 GitHub 仓库"
            for (apiUrl in apiUrls) {
                var conn: HttpURLConnection? = null
                try {
                    conn = openConnectionWithRedirects(apiUrl, 6000)
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

                    if (conn.responseCode !in 200..299) {
                        lastError = "获取版本信息失败 (HTTP ${conn.responseCode})"
                        conn.disconnect()
                        continue
                    }

                    val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    conn.disconnect()

                    val root = JSONObject(jsonStr)
                    val releaseName = root.optString("name", "最新热补丁")
                    val body = root.optString("body", "包含关键性能优化与问题修复")
                    val assets = root.optJSONArray("assets") ?: JSONArray()

                    var patchUrl = ""
                    var patchSize = 0L
                    var patchVer = ""

                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        // 匹配形如 collecter-patch-v4.1.0.zip 或 patch-v4.1.0.zip
                        if (name.contains("patch") && name.endsWith(".zip")) {
                            patchUrl = asset.optString("browser_download_url", "")
                            patchSize = asset.optLong("size", 0L)
                            val match = Regex("v?([0-9]+\\.[0-9]+\\.[0-9]+)").find(name)
                            patchVer = match?.groupValues?.getOrNull(1) ?: "4.1.0"
                            break
                        }
                    }

                    if (patchUrl.isBlank()) {
                        (context as? Activity)?.runOnUiThread {
                            onResult(null, "当前已是最新状态，暂无待更新增量补丁")
                        }
                        return@execute
                    }

                    if (activePatchVer != null && compareVersions(patchVer, activePatchVer) <= 0) {
                        (context as? Activity)?.runOnUiThread {
                            onResult(null, "已应用最新补丁 (v$activePatchVer)，无需重复更新")
                        }
                        return@execute
                    }

                    val info = HotPatchInfo(
                        patchVersion = patchVer,
                        targetBaseVersion = baseVer,
                        downloadUrl = patchUrl,
                        sizeBytes = patchSize,
                        md5 = "",
                        changelog = body,
                        title = releaseName
                    )

                    (context as? Activity)?.runOnUiThread {
                        onResult(info, null)
                    }
                    return@execute
                } catch (e: Exception) {
                    conn?.disconnect()
                    lastError = "网络连接异常: ${e.localizedMessage}"
                }
            }

            (context as? Activity)?.runOnUiThread {
                onResult(null, lastError)
            }
        }
    }

    fun downloadAndApplyPatch(
        context: Context,
        info: HotPatchInfo,
        onProgress: (percent: Int, speedKb: Long) -> Unit,
        onCompleted: (Boolean, String?) -> Unit
    ) {
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        executor.execute {
            val tempDir = File(context.cacheDir, "patch_download").apply { mkdirs() }
            val tempFile = File(tempDir, "patch_${info.patchVersion}.zip")

            val urlsToTry = listOf(
                "https://ghfast.top/${info.downloadUrl}",
                "https://mirror.ghproxy.com/${info.downloadUrl}",
                "https://ghproxy.net/${info.downloadUrl}",
                info.downloadUrl
            )

            var lastError = "下载失败"
            var success = false

            for (urlStr in urlsToTry) {
                var conn: HttpURLConnection? = null
                try {
                    if (tempFile.exists()) tempFile.delete()

                    conn = openConnectionWithRedirects(urlStr, 7000)
                    if (conn.responseCode !in 200..299) {
                        lastError = "HTTP ${conn.responseCode}"
                        conn.disconnect()
                        continue
                    }

                    val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else info.sizeBytes
                    val inStream = conn.inputStream
                    val outStream = FileOutputStream(tempFile)

                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var lastTime = System.currentTimeMillis()
                    var bytesInSecond = 0L

                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesInSecond += read

                        val now = System.currentTimeMillis()
                        if (now - lastTime >= 400 || downloadedBytes == totalBytes) {
                            val speedKb = (bytesInSecond * 1000) / ((now - lastTime).coerceAtLeast(1) * 1024)
                            val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                            (context as? Activity)?.runOnUiThread {
                                onProgress(percent.coerceIn(0, 100), speedKb)
                            }
                            lastTime = now
                            bytesInSecond = 0L
                        }
                    }

                    outStream.flush()
                    outStream.close()
                    inStream.close()
                    conn.disconnect()

                    // 应用热补丁
                    val patchApplied = HotPatchEngine.applyPatchZip(context, tempFile)
                    tempFile.delete()

                    if (patchApplied) {
                        success = true
                        (context as? Activity)?.runOnUiThread {
                            onCompleted(true, null)
                        }
                        break
                    } else {
                        lastError = "补丁包校验或解压失败"
                    }
                } catch (e: Exception) {
                    conn?.disconnect()
                    tempFile.delete()
                    lastError = e.localizedMessage ?: "网络连接异常"
                }
            }

            if (!success) {
                tempFile.delete()
                (context as? Activity)?.runOnUiThread {
                    onCompleted(false, "下载失败: $lastError\n请检查网络或稍后重试")
                }
            }
        }
    }

    /** 智能跟随重定向的 HTTP 连接器 */
    private fun openConnectionWithRedirects(initialUrl: String, timeoutMs: Int): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 5) {
            val url = URI.create(currentUrl).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Collecter-HotPatch-Client/4.1.0 (Android; Mobile)")
                setRequestProperty("Accept", "*/*")
            }

            val code = conn.responseCode
            if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URI.create(currentUrl).resolve(location).toString()
                    }
                    redirects++
                    continue
                }
            }
            return conn
        }
        throw Exception("重定向过多 ($initialUrl)")
    }

    fun checkSilently(activity: Activity) {
        checkHotPatch(activity) { info, _ ->
            if (info != null) {
                HotUpdateDialog.show(activity, info)
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val num1 = p1.getOrElse(i) { 0 }
            val num2 = p2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }
}
