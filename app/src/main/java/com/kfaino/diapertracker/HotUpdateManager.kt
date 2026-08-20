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
        val baseVer = UpdateManager.getAppVersionName(context)
        val activePatchVer = HotPatchEngine.getActivePatchVersion(context)
        val store = DataStore(context)
        val repo = store.getGithubRepo().ifBlank { "Workworks/Collector" }
        val apiUrls = listOf(
            "https://api.github.com/repos/$repo/releases/latest",
            "https://ghfast.top/https://api.github.com/repos/$repo/releases/latest",
            "https://mirror.ghproxy.com/https://api.github.com/repos/$repo/releases/latest"
        )

        executor.execute {
            var lastError = "无法连接至 GitHub 仓库"
            for (apiUrl in apiUrls) {
                try {
                    val conn = URI.create(apiUrl).toURL().openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("User-Agent", "Collecter-HotPatch-Client")
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

                    if (conn.responseCode != 200) {
                        lastError = "获取版本信息失败 (HTTP ${conn.responseCode})"
                        continue
                    }

                    val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
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
                        // 匹配形如 collecter-patch-v3.0.1.zip 或 patch-v3.0.1.zip
                        if (name.contains("patch") && name.endsWith(".zip")) {
                            patchUrl = asset.optString("browser_download_url", "")
                            patchSize = asset.optLong("size", 0L)
                            val match = Regex("v?([0-9]+\\.[0-9]+\\.[0-9]+)").find(name)
                            patchVer = match?.groupValues?.getOrNull(1) ?: "3.0.1"
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
        executor.execute {
            var tempFile: File? = null
            try {
                val tempDir = File(context.cacheDir, "patch_download").apply { mkdirs() }
                tempFile = File(tempDir, "patch_${info.patchVersion}.zip")

                val conn = URI.create(info.downloadUrl).toURL().openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Collecter-HotPatch-Client")

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
                    if (now - lastTime >= 500) {
                        val speedKb = (bytesInSecond * 1000) / ((now - lastTime) * 1024)
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

                // 应用热补丁
                val success = HotPatchEngine.applyPatchZip(context, tempFile)
                tempFile.delete()

                (context as? Activity)?.runOnUiThread {
                    if (success) {
                        onCompleted(true, null)
                    } else {
                        onCompleted(false, "补丁包校验或解压失败")
                    }
                }
            } catch (e: Exception) {
                tempFile?.delete()
                (context as? Activity)?.runOnUiThread {
                    onCompleted(false, "下载失败: ${e.localizedMessage}")
                }
            }
        }
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
