package com.kfaino.diapertracker

import android.content.Context
import android.content.SharedPreferences
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 类游戏级动态热补丁与沙盒资源加载引擎 (Hot Patch Engine)
 * - 维护私有安全沙盒 /files/hot_patches/
 * - 支持动态 Dex 加载、Web 大屏控制台热替换与动态模板注入
 * - 内置崩溃熔断计数与自动回滚保护机制
 */
object HotPatchEngine {

    private const val PREFS_NAME = "hot_patch_prefs"
    private const val KEY_PATCH_VERSION = "active_patch_version"
    private const val KEY_PATCH_TARGET_BASE = "active_patch_target_base"
    private const val KEY_CRASH_COUNT = "patch_consecutive_crash_count"
    private const val MAX_CRASH_THRESHOLD = 2

    private var activeDexClassLoader: DexClassLoader? = null
    private var activePatchManifest: JSONObject? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)

        // 崩溃熔断保护：若连续崩溃超过阈值，自动安全回滚
        if (crashCount >= MAX_CRASH_THRESHOLD) {
            rollback(context)
            prefs.edit().putInt(KEY_CRASH_COUNT, 0).apply()
            return
        }

        // 启动时临时增加崩溃计数，正常运行 5 秒后清零
        prefs.edit().putInt(KEY_CRASH_COUNT, crashCount + 1).apply()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            prefs.edit().putInt(KEY_CRASH_COUNT, 0).apply()
        }, 5000)

        val patchDir = getActivePatchDir(context)
        val manifestFile = File(patchDir, "manifest.json")
        if (manifestFile.exists()) {
            try {
                val jsonStr = manifestFile.readText(StandardCharsets.UTF_8)
                activePatchManifest = JSONObject(jsonStr)

                // 挂载动态 Dex（若存在）
                val dexFile = File(patchDir, "patch.dex")
                if (dexFile.exists()) {
                    val optDir = File(context.cacheDir, "patch_opt").apply { mkdirs() }
                    activeDexClassLoader = DexClassLoader(
                        dexFile.absolutePath,
                        optDir.absolutePath,
                        null,
                        context.classLoader
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getActivePatchVersion(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PATCH_VERSION, null)
    }

    fun getActiveWebDashboardHtml(context: Context): String? {
        val patchDir = getActivePatchDir(context)
        val webHtmlFile = File(patchDir, "web/index.html")
        return if (webHtmlFile.exists()) {
            try {
                webHtmlFile.readText(StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 将下载的增量补丁 zip 包解压并激活
     */
    fun applyPatchZip(context: Context, patchZip: File): Boolean {
        try {
            val activeDir = getActivePatchDir(context)
            val backupDir = getRollbackPatchDir(context)

            // 1. 将旧补丁备份至 rollback 目录
            if (activeDir.exists()) {
                backupDir.deleteRecursively()
                activeDir.renameTo(backupDir)
            }

            activeDir.mkdirs()

            // 2. 解压新补丁
            unzip(patchZip, activeDir)

            // 3. 校验新补丁 manifest
            val manifestFile = File(activeDir, "manifest.json")
            if (!manifestFile.exists()) {
                // 补丁不完整，回滚
                activeDir.deleteRecursively()
                if (backupDir.exists()) backupDir.renameTo(activeDir)
                return false
            }

            val manifest = JSONObject(manifestFile.readText(StandardCharsets.UTF_8))
            val patchVer = manifest.optString("patchVersion", "1.0")
            val targetBase = manifest.optString("targetBaseVersion", "3.0.0")

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PATCH_VERSION, patchVer)
                .putString(KEY_PATCH_TARGET_BASE, targetBase)
                .putInt(KEY_CRASH_COUNT, 0)
                .apply()

            activePatchManifest = manifest
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /** 安全回滚至 Base 状态或上一稳定补丁 */
    fun rollback(context: Context): Boolean {
        val activeDir = getActivePatchDir(context)
        val backupDir = getRollbackPatchDir(context)

        activeDir.deleteRecursively()
        if (backupDir.exists()) {
            backupDir.renameTo(activeDir)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PATCH_VERSION).remove(KEY_PATCH_TARGET_BASE).apply()
        activeDexClassLoader = null
        activePatchManifest = null
        return true
    }

    private fun getActivePatchDir(context: Context): File {
        return File(context.filesDir, "hot_patches/active")
    }

    private fun getRollbackPatchDir(context: Context): File {
        return File(context.filesDir, "hot_patches/rollback")
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val outFile = File(targetDir, entry!!.name)
                if (entry!!.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }

    fun calculateMd5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
