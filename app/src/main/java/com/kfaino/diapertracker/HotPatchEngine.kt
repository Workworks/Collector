package com.kfaino.diapertracker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * 类游戏级动态热补丁与沙盒资源加载引擎 (Hot Patch Engine)
 * - 维护私有安全沙盒 /files/hot_patches/
 * - 支持动态 Dex 加载、Web 大屏控制台热替换与动态模板注入
 * - 内置崩溃熔断计数与自动回滚保护机制
 *
 * ## 🔐 安全不变量（见 GEMINI.md 铁律 3，修改前必须先问用户）
 * 1. **动态 dex 一律先验签后加载**：补丁包内的 `patch.dex` 必须附带 `patch.dex.sig`
 *    （对 dex 原文的 SHA256withRSA 签名，Base64 编码），并能被内置公钥 [PATCH_PUBLIC_KEY_B64] 验签通过。
 *    验签失败、签名缺失、公钥未配置 —— 一律**拒绝加载并自动回滚**（fail-closed）。
 * 2. **MD5 不是验签**：[calculateMd5] 仅用于文件指纹/去重，**禁止**把它当作安全校验使用。
 * 3. **解压必须防路径穿越**：见 [unzip] 的 Zip Slip 校验与体积上限。
 *
 * ## 如何签发补丁（配置公钥后才允许下发含 dex 的补丁）
 * ```bash
 * # 1) 生成一次性密钥对（私钥务必离线保管，绝不入库）
 * openssl genrsa -out patch_private.pem 3072
 * openssl rsa -in patch_private.pem -pubout -outform DER -out patch_public.der
 * base64 -w0 patch_public.der          # 输出填入 PATCH_PUBLIC_KEY_B64
 *
 * # 2) 每次签发补丁
 * openssl dgst -sha256 -sign patch_private.pem -out patch.dex.sig.bin patch.dex
 * base64 -w0 patch.dex.sig.bin > patch.dex.sig   # 与 patch.dex 一起打进补丁 zip
 * ```
 */
object HotPatchEngine {

    private const val TAG = "HotPatchEngine"

    private const val PREFS_NAME = "hot_patch_prefs"
    private const val KEY_PATCH_VERSION = "active_patch_version"
    private const val KEY_PATCH_TARGET_BASE = "active_patch_target_base"
    private const val KEY_CRASH_COUNT = "patch_consecutive_crash_count"
    private const val MAX_CRASH_THRESHOLD = 2

    private const val DEX_NAME = "patch.dex"
    private const val DEX_SIG_NAME = "patch.dex.sig"

    /**
     * 补丁签名公钥（X.509 DER 的 Base64）。
     *
     * ⚠️ **留空 = 禁止一切动态 dex 加载**（fail-closed，这是安全的默认值）。
     * 只有当你确实需要下发含 dex 的热补丁时，才按上文步骤生成密钥对并把公钥填到这里。
     * 纯资源补丁（web 大屏 HTML、模板）不受此限制，因为它们不在本进程内执行代码。
     */
    private const val PATCH_PUBLIC_KEY_B64 = ""

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
                // 🔐 安全不变量：必须先验签。验签不通过一律拒绝加载并回滚（fail-closed）
                val dexFile = File(patchDir, DEX_NAME)
                if (dexFile.exists()) {
                    if (!verifyDexSignature(dexFile, File(patchDir, DEX_SIG_NAME))) {
                        Log.e(TAG, "补丁 dex 验签失败或公钥未配置，拒绝加载并回滚至安全状态")
                        activePatchManifest = null
                        rollback(context)
                        return
                    }
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
        val activeDir = getActivePatchDir(context)
        val backupDir = getRollbackPatchDir(context)

        // 失败时把备份原样搬回来，避免「新补丁没装上、旧补丁也丢了」
        fun restoreBackup() {
            activeDir.deleteRecursively()
            if (backupDir.exists()) backupDir.renameTo(activeDir)
        }

        try {
            // 1. 将旧补丁备份至 rollback 目录
            if (activeDir.exists()) {
                backupDir.deleteRecursively()
                activeDir.renameTo(backupDir)
            }

            activeDir.mkdirs()

            // 2. 解压新补丁（unzip 内含 Zip Slip 与体积上限校验，非法条目会直接抛出）
            unzip(patchZip, activeDir)

            // 3. 校验新补丁 manifest
            val manifestFile = File(activeDir, "manifest.json")
            if (!manifestFile.exists()) {
                Log.w(TAG, "补丁缺少 manifest.json，判定为不完整补丁，已回滚")
                restoreBackup()
                return false
            }

            // 4. 安全不变量：补丁若携带 dex，必须验签通过才允许落地
            val dexFile = File(activeDir, DEX_NAME)
            if (dexFile.exists() && !verifyDexSignature(dexFile, File(activeDir, DEX_SIG_NAME))) {
                Log.e(TAG, "补丁 dex 验签失败或公钥未配置，拒绝安装并已回滚")
                restoreBackup()
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
            Log.e(TAG, "应用热补丁失败，已回滚至上一稳定状态", e)
            restoreBackup()
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

    /**
     * 安全解压，委托给纯 JVM 的 [PatchArchive]（含 Zip Slip 与 zip 炸弹防护，可被单元测试直接覆盖）。
     *
     * 任一安全校验不通过都会抛出 [SecurityException]，由调用方回滚。
     */
    private fun unzip(zipFile: File, targetDir: File) = PatchArchive.safeUnzip(zipFile, targetDir)

    /**
     * 校验动态 dex 的 RSA 签名（SHA256withRSA）。
     *
     * fail-closed：以下任一情况都返回 false，调用方必须拒绝加载并回滚。
     * - [PATCH_PUBLIC_KEY_B64] 未配置（默认状态，等同于「本应用不接受任何动态 dex」）；
     * - 签名文件缺失或格式非法；
     * - 签名与 dex 内容不匹配。
     */
    private fun verifyDexSignature(dexFile: File, sigFile: File): Boolean {
        val valid = DexSignatureVerifier.verify(dexFile, sigFile, PATCH_PUBLIC_KEY_B64)
        if (!valid) Log.e(TAG, "补丁 DEX 验签失败、缺少签名或未配置公钥，拒绝加载")
        return valid
    }

    /**
     * 文件 MD5 指纹。
     *
     * 仅用于**去重与缓存命中判断**，**不是安全校验**。
     * 安全校验请使用 verifyDexSignature。
     */
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
        } catch (e: Exception) {
            Log.w(TAG, "计算文件 MD5 失败：" + file.name, e)
            ""
        }
    }
}
