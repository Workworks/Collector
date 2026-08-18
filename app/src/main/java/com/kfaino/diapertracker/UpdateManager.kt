package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.kfaino.diapertracker.databinding.DialogCustomCheckingBinding
import com.kfaino.diapertracker.databinding.DialogCustomResultBinding
import com.kfaino.diapertracker.databinding.DialogCustomUpdateAvailableBinding
import com.kfaino.diapertracker.databinding.DialogCustomUpdateProgressBinding
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
 * 在线更新管理器（支持后台静默预下载、0秒极速秒装、高定弹窗、多镜像加速与断点校验）
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

    /** 获取本地应用当前 VersionName */
    fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "2.2.6"
        } catch (_: Exception) {
            "2.2.6"
        }
    }

    /** 比较远端版本与当前本地版本 */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val rParts = remoteVersion.trim().removePrefix("v").removePrefix("V").split(".")
            val cParts = currentVersion.trim().removePrefix("v").removePrefix("V").split(".")
            val len = maxOf(rParts.size, cParts.size)
            for (i in 0 until len) {
                val rNum = rParts.getOrNull(i)?.toIntOrNull() ?: 0
                val cNum = cParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (rNum > cNum) return true
                if (rNum < cNum) return false
            }
            return false
        } catch (_: Exception) {
            return remoteVersion != currentVersion
        }
    }

    /** 创建无原生黑灰底边框的高定现代弹窗 */
    private fun createCustomDialog(activity: Activity, view: View, cancelable: Boolean = false): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setCancelable(cancelable)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        return dialog
    }

    /**
     * 后台静默预下载（零感知将新版安装包拉取至本地缓存，用户点击升级时 0 秒直接秒装）
     */
    fun preloadSilently(context: Context) {
        executor.execute {
            try {
                val store = DataStore(context)
                val repo = store.getGithubRepo()
                val currentVer = getAppVersionName(context)
                val release = fetchLatestRelease(repo) ?: return@execute
                if (!isNewerVersion(release.versionName, currentVer)) return@execute

                val saveDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val apkFile = File(saveDir, "Collecter_${release.versionName}.apk")
                if (apkFile.exists() && apkFile.length() > 0 && (release.apkSize == 0L || apkFile.length() == release.apkSize)) {
                    return@execute // 已经静默下载完成
                }

                val tempFile = File(saveDir, "Collecter_${release.versionName}.apk.tmp")
                val urlsToTry = listOf(
                    release.apkDownloadUrl,
                    "https://ghfast.top/${release.apkDownloadUrl}",
                    "https://mirror.ghproxy.com/${release.apkDownloadUrl}"
                )

                for (currentUrl in urlsToTry) {
                    try {
                        val url = URL(currentUrl)
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) CollecterApp")
                        }
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (tempFile.exists() && tempFile.length() > 0) {
                                if (apkFile.exists()) apkFile.delete()
                                tempFile.renameTo(apkFile)
                                break
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 检查更新
     * @param activity 当前 Activity
     * @param isManual true 表示用户手动点击；false 表示自动静默检测
     */
    fun checkUpdate(activity: Activity, isManual: Boolean = true) {
        val store = DataStore(activity)
        val repo = store.getGithubRepo()
        val currentVer = getAppVersionName(activity)

        var checkingDialog: AlertDialog? = null
        if (isManual) {
            val checkingBinding = DialogCustomCheckingBinding.inflate(LayoutInflater.from(activity))
            checkingDialog = createCustomDialog(activity, checkingBinding.root, cancelable = false)
            checkingDialog.show()
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
                            showUpToDateDialog(activity, currentVer)
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
     * 请求 GitHub Releases API（多镜像备用与直连通道）
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
                    connectTimeout = 5000
                    readTimeout = 5000
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
            throw lastException ?: Exception("连接 GitHub 失败，请检查网络连接")
        }

        val json = JSONObject(jsonText)

        val tagName = json.optString("tag_name", "")
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val title = json.optString("name", "新版本 $tagName")
        val changelog = json.optString("body", "暂无更新说明").trim()
        val publishedAt = formatIsoDate(json.optString("published_at", ""))
        val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

        var apkUrl = ""
        var apkSize = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
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
     * 定制现代发现新版本弹窗 (支持秒装就绪判断)
     */
    private fun showUpdateAvailableDialog(
        activity: Activity,
        release: ReleaseInfo,
        currentVer: String
    ) {
        val binding = DialogCustomUpdateAvailableBinding.inflate(LayoutInflater.from(activity))
        val dialog = createCustomDialog(activity, binding.root, cancelable = true)

        binding.customVersionBadge.text = "${release.tagName} (当前 v$currentVer)"
        binding.customUpdateSize.text = if (release.apkSize > 0) formatFileSize(release.apkSize) else ""
        binding.customUpdateDate.text = release.publishedAt.ifEmpty { "最新构建" }
        binding.customUpdateChangelog.text = release.changelog.ifEmpty { "优化部分体验与修复已知问题。" }
        binding.customBtnCancel.applyPressScaleAnimation(0.92f)
        binding.customBtnUpdate.applyPressScaleAnimation(0.92f)

        binding.customBtnCancel.setOnClickListener {
            dialog.dismiss()
        }

        val saveDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.cacheDir
        val apkFile = File(saveDir, "Collecter_${release.versionName}.apk")
        val isPreloaded = apkFile.exists() && apkFile.length() > 0 && (release.apkSize == 0L || apkFile.length() == release.apkSize)

        if (isPreloaded) {
            binding.customBtnUpdate.text = "⚡ 安装包已就绪，立即秒装"
            binding.customBtnUpdate.setOnClickListener {
                dialog.dismiss()
                installApk(activity, apkFile)
            }
        } else {
            binding.customBtnUpdate.text = "🚀 立即极速更新"
            binding.customBtnUpdate.setOnClickListener {
                dialog.dismiss()
                startDownloadAndInstall(activity, release)
            }
        }

        dialog.show()
    }

    /**
     * 定制现代极速下载与安装流程
     */
    private fun startDownloadAndInstall(activity: Activity, release: ReleaseInfo) {
        val progressBinding = DialogCustomUpdateProgressBinding.inflate(LayoutInflater.from(activity))
        progressBinding.customProgressStatus.text = "正在极速下载 ${release.tagName}..."
        progressBinding.customDownloadProgressBar.progress = 0
        progressBinding.customProgressPercent.text = "0%"
        progressBinding.customProgressSize.text = "0.0 MB / ${formatFileSize(release.apkSize)}"

        var isCanceled = false
        var downloadThread: Thread? = null

        val progressDialog = createCustomDialog(activity, progressBinding.root, cancelable = false)

        progressBinding.customBtnCancelDownload.applyPressScaleAnimation(0.92f)
        progressBinding.customBtnCancelDownload.setOnClickListener {
            isCanceled = true
            downloadThread?.interrupt()
            progressDialog.dismiss()
        }

        progressDialog.show()

        val saveDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.cacheDir
        val apkFile = File(saveDir, "Collecter_${release.versionName}.apk")

        val urlsToTry = listOf(
            release.apkDownloadUrl,
            "https://ghfast.top/${release.apkDownloadUrl}",
            "https://mirror.ghproxy.com/${release.apkDownloadUrl}"
        )

        downloadThread = Thread {
            var lastError: Exception? = null
            var success = false

            for (currentUrl in urlsToTry) {
                if (isCanceled) break
                try {
                    val url = URL(currentUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) CollecterApp")
                    }

                    val responseCode = conn.responseCode
                    if (responseCode !in 200..299) {
                        throw Exception("HTTP $responseCode")
                    }

                    val totalLength = conn.contentLength.toLong().let { if (it > 0) it else release.apkSize }
                    val input = conn.inputStream
                    val output = FileOutputStream(apkFile)

                    val buffer = ByteArray(8192)
                    var read: Int
                    var downloaded = 0L
                    var lastUiUpdate = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        if (isCanceled) {
                            output.close()
                            input.close()
                            apkFile.delete()
                            return@Thread
                        }
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdate > 80 || downloaded == totalLength) {
                            lastUiUpdate = now
                            val percent = if (totalLength > 0) (downloaded * 100 / totalLength).toInt() else 0
                            val dlMb = downloaded.toDouble() / (1024 * 1024)
                            val totalMb = totalLength.toDouble() / (1024 * 1024)

                            mainHandler.post {
                                if (!progressDialog.isShowing) return@post
                                progressBinding.customDownloadProgressBar.progress = percent
                                progressBinding.customProgressPercent.text = "$percent%"
                                progressBinding.customProgressSize.text = String.format(Locale.getDefault(), "%.1f MB / %.1f MB", dlMb, totalMb)
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

    /** 定制当前已是最新弹窗 */
    private fun showUpToDateDialog(activity: Activity, currentVer: String) {
        val binding = DialogCustomResultBinding.inflate(LayoutInflater.from(activity))
        val dialog = createCustomDialog(activity, binding.root, cancelable = true)

        binding.resultTitle.text = "当前已是最新版本"
        binding.resultMessage.text = "当前应用版本为 v$currentVer\n已包含所有最新功能与性能优化，无需更新。"
        binding.resultBtnConfirm.applyPressScaleAnimation(0.92f)
        binding.resultBtnConfirm.text = "好的"
        binding.resultBtnConfirm.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 定制检查失败弹窗 */
    private fun showErrorDialog(activity: Activity, repo: String, errorMsg: String) {
        val binding = DialogCustomResultBinding.inflate(LayoutInflater.from(activity))
        val dialog = createCustomDialog(activity, binding.root, cancelable = true)

        binding.resultTitle.text = "检查更新失败"
        binding.resultMessage.text = "目标仓库: $repo\n原因: $errorMsg\n\n提示: 可在“我的”页面检查网络连接或配置 GitHub 仓库。"
        binding.resultBtnConfirm.applyPressScaleAnimation(0.92f)
        binding.resultBtnConfirm.text = "我知道了"
        binding.resultBtnConfirm.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 调起系统安装器安装 APK */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
                if (!hasInstallPermission) {
                    val binding = DialogCustomResultBinding.inflate(LayoutInflater.from(context))
                    val dialog = MaterialAlertDialogBuilder(context)
                        .setView(binding.root)
                        .setCancelable(true)
                        .create()
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    binding.resultTitle.text = "需要安装应用权限"
                    binding.resultMessage.text = "为完成更新安装，请在随后的系统设置页面中开启【允许安装未知应用】权限。"
                    binding.resultBtnConfirm.text = "前往开启"
                    binding.resultBtnConfirm.setOnClickListener {
                        dialog.dismiss()
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    dialog.show()
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
            Toast.makeText(context, "调起安装失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    private fun formatIsoDate(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(iso) ?: return iso
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            iso
        }
    }
}
