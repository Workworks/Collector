package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogUpdateFoundBinding
import com.kfaino.diapertracker.databinding.DialogUpdateProgressBinding
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/**
 * GitHub Releases 在线热更新管理器
 * 支持从 GitHub 仓库自动检测最新 Release 版本、下载 APK 并通过 FileProvider 调用系统安装程序完成升级
 */
object UpdateManager {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val title: String,
        val changelog: String,
        val apkDownloadUrl: String,
        val apkSize: Long,
        val publishedAt: String,
        val htmlUrl: String
    )

    /**
     * 获取当前 App 版本名称 (如 "2.1")
     */
    fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    /**
     * 版本号比较：如果 remoteVersion 大于 currentVersion 则返回 true
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.removePrefix("v").removePrefix("V").split(".")
            val currentParts = currentVersion.removePrefix("v").removePrefix("V").split(".")

            val maxLength = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val rNum = remoteParts.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val cNum = currentParts.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                if (rNum > cNum) return true
                if (rNum < cNum) return false
            }
            return false
        } catch (_: Exception) {
            return remoteVersion != currentVersion
        }
    }

    /**
     * 检查更新
     * @param activity 当前 Activity
     * @param isManual true 表示用户手动点击（需要展示“正在检查”及“当前已是最新”等提示）；false 表示自动静默检测
     */
    fun checkUpdate(activity: Activity, isManual: Boolean = true) {
        val store = DataStore(activity)
        val repo = store.getGithubRepo()
        val currentVer = getAppVersionName(activity)

        var checkingDialog: AlertDialog? = null
        if (isManual) {
            checkingDialog = MaterialAlertDialogBuilder(activity)
                .setTitle("检查更新")
                .setMessage("正在连接 GitHub 检测最新版本...")
                .setCancelable(false)
                .show()
        }

        executor.execute {
            var errorMsg: String? = null
            var releaseInfo: ReleaseInfo? = null

            try {
                releaseInfo = fetchLatestRelease(repo)
            } catch (e: Exception) {
                errorMsg = e.message ?: "网络请求失败"
            }

            mainHandler.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                checkingDialog?.dismiss()

                if (releaseInfo != null) {
                    val hasNew = isNewerVersion(releaseInfo.versionName, currentVer)
                    if (hasNew) {
                        showUpdateAvailableDialog(activity, releaseInfo, currentVer)
                    } else {
                        if (isManual) {
                            Toast.makeText(
                                activity,
                                "当前已是最新版本 (v$currentVer)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    if (isManual) {
                        showErrorDialog(activity, repo, errorMsg ?: "未检测到可用的更新发布包")
                    }
                }
            }
        }
    }

    /**
     * 请求 GitHub Releases API（支持多镜像备用通道）
     */
    private fun fetchLatestRelease(repo: String): ReleaseInfo? {
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        val apiUrls = listOf(
            "https://api.github.com/repos/$repo/releases/latest",
            "https://ghfast.top/https://api.github.com/repos/$repo/releases/latest",
            "https://mirror.ghproxy.com/https://api.github.com/repos/$repo/releases/latest"
        )

        var lastException: Exception? = null
        var jsonText = ""

        for (apiUrl in apiUrls) {
            try {
                val url = URL(apiUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) CollecterApp")
                }

                val code = conn.responseCode
                if (code == 404) {
                    throw Exception("GitHub 仓库【$repo】未找到或尚无公开的 Release 版本")
                }
                if (code in 200..299) {
                    jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                    if (jsonText.isNotBlank()) break
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (jsonText.isEmpty()) {
            throw lastException ?: Exception("连接 GitHub 失败，请检查网络")
        }

        val json = JSONObject(jsonText)

        val tagName = json.optString("tag_name", "")
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val title = json.optString("name", "新版本 $tagName")
        val changelog = json.optString("body", "暂无更新说明").trim()
        val publishedAt = formatIsoDate(json.optString("published_at", ""))
        val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

        // 查找资产中的 .apk 文件
        var apkUrl = ""
        var apkSize = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        if (apkUrl.isEmpty()) {
            throw Exception("最新 Release ($tagName) 中未包含可供安装的 .apk 附件")
        }

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            title = title,
            changelog = changelog,
            apkDownloadUrl = apkUrl,
            apkSize = apkSize,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl
        )
    }

    /**
     * 发现新版本弹窗
     */
    private fun showUpdateAvailableDialog(
        activity: Activity,
        release: ReleaseInfo,
        currentVer: String
    ) {
        val binding = DialogUpdateFoundBinding.inflate(LayoutInflater.from(activity))
        binding.updateVersionBadge.text = "${release.tagName} (当前 v$currentVer)"
        binding.updateSize.text = if (release.apkSize > 0) formatFileSize(release.apkSize) else ""
        binding.updateDate.text = release.publishedAt
        binding.updateChangelog.text = release.changelog.ifEmpty { "优化部分体验与修复已知问题。" }

        MaterialAlertDialogBuilder(activity)
            .setTitle("发现新版本 (${release.tagName})")
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("立即下载更新") { _, _ ->
                startDownloadAndInstall(activity, release)
            }
            .setNeutralButton("浏览器下载") { _, _ ->
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                    activity.startActivity(browserIntent)
                } catch (_: Exception) {
                    Toast.makeText(activity, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("稍后再说", null)
            .show()
    }

    /**
     * 开始下载并安装 APK
     */
    private fun startDownloadAndInstall(activity: Activity, release: ReleaseInfo) {
        val progressBinding = DialogUpdateProgressBinding.inflate(LayoutInflater.from(activity))
        progressBinding.progressStatus.text = "正在下载 ${release.tagName}..."
        progressBinding.downloadProgressBar.progress = 0
        progressBinding.progressPercent.text = "0%"
        progressBinding.progressSize.text = "0 MB / ${formatFileSize(release.apkSize)}"

        var isCanceled = false
        var downloadThread: Thread? = null

        val progressDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("下载更新")
            .setView(progressBinding.root)
            .setCancelable(false)
            .setNegativeButton("取消") { _, _ ->
                isCanceled = true
                downloadThread?.interrupt()
            }
            .create()

        progressDialog.show()

        val saveDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.cacheDir
        val apkFile = File(saveDir, "Collecter_${release.versionName}.apk")

        // 尝试直接下载，若失败可使用国内镜像加速
        val urlsToTry = listOf(
            release.apkDownloadUrl,
            "https://ghfast.top/${release.apkDownloadUrl}",
            "https://mirror.ghproxy.com/${release.apkDownloadUrl}"
        )

        downloadThread = Thread {
            var success = false
            var lastError: Exception? = null

            for (downloadUrl in urlsToTry) {
                if (isCanceled) break
                try {
                    val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 DiaperTracker")
                    }

                    // 处理重定向
                    var responseCode = conn.responseCode
                    var currentConn = conn
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == 307 || responseCode == 308) {
                        val newLocation = currentConn.getHeaderField("Location")
                        if (!newLocation.isNullOrEmpty()) {
                            currentConn = (URL(newLocation).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 15000
                                readTimeout = 15000
                                setRequestProperty("User-Agent", "Mozilla/5.0 DiaperTracker")
                            }
                            responseCode = currentConn.responseCode
                        }
                    }

                    if (responseCode !in 200..299) {
                        throw Exception("HTTP $responseCode")
                    }

                    val totalBytes = if (currentConn.contentLengthLong > 0) currentConn.contentLengthLong else release.apkSize
                    val input = currentConn.inputStream
                    val output = FileOutputStream(apkFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = 0L
                    var lastUpdateTs = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCanceled) {
                            output.close()
                            input.close()
                            apkFile.delete()
                            return@Thread
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTs > 100 || downloadedBytes == totalBytes) {
                            lastUpdateTs = now
                            val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                            mainHandler.post {
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    progressBinding.downloadProgressBar.progress = percent
                                    progressBinding.progressPercent.text = "$percent%"
                                    progressBinding.progressSize.text = "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
                                }
                            }
                        }
                    }

                    output.flush()
                    output.close()
                    input.close()
                    success = true
                    break
                } catch (e: Exception) {
                    lastError = e
                }
            }

            mainHandler.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                progressDialog.dismiss()

                if (success && apkFile.exists() && apkFile.length() > 0) {
                    installApk(activity, apkFile)
                } else if (!isCanceled) {
                    Toast.makeText(
                        activity,
                        "下载失败: ${lastError?.message ?: "网络连接超时"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        downloadThread.start()
    }

    /**
     * 调起系统安装器安装 APK
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            // Android 8.0+ 安装未知来源应用权限判断
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
                if (!hasInstallPermission) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("需要安装权限")
                        .setMessage("为完成更新安装，请在随后的设置页面中允许【安装未知应用】权限。")
                        .setPositiveButton("去开启") { _, _ ->
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "调起安装程序失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 错误提示对话框
     */
    private fun showErrorDialog(context: Context, repo: String, message: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("检查更新失败")
            .setMessage("目标仓库：$repo\n原因：$message\n\n提示：可在“我的”页面中点击更新项配置或修改 GitHub 仓库地址。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 MB"
        val mb = sizeBytes.toDouble() / (1024 * 1024)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    private fun formatIsoDate(isoString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString)
            if (date != null) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            } else {
                "近期"
            }
        } catch (_: Exception) {
            "近期"
        }
    }
}
