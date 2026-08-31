package com.kfaino.diapertracker

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log

/**
 * 📸 系统截图无感监听助手 (Screenshot Watcher Helper)
 * - 注册 ContentObserver 监听媒体库变化
 * - 自动识别 Screenshots/截屏 目录下的新图并去重
 * - 静默移交 ScreenshotOcrProcessor 进行本地离线 OCR 与智能归档
 */
object ScreenshotWatcherHelper {

    private const val TAG = "ScreenshotWatcherHelper"
    private var observer: ContentObserver? = null
    private val processedUris = mutableSetOf<String>()
    private var isListening = false

    fun startListening(context: Context) {
        if (isListening) return
        if (!DataStore(context).isScreenshotCaptureEnabled()) return
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_IMAGES" else "android.permission.READ_EXTERNAL_STORAGE"
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "未授予媒体读取权限；请使用系统分享将截图保存到收集箱")
            return
        }
        val appContext = context.applicationContext

        val handler = Handler(Looper.getMainLooper())
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null) {
                    handleMediaChange(appContext, uri)
                } else {
                    handleLatestMedia(appContext)
                }
            }
        }

        try {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer!!
            )
            isListening = true
            Log.i(TAG, "📸 截图无感监听器已启动")
        } catch (e: Exception) {
            Log.w(TAG, "注册截图监听器失败: ${e.message}", e)
        }
    }

    fun stopListening(context: Context) {
        if (!isListening) return
        try {
            observer?.let { context.applicationContext.contentResolver.unregisterContentObserver(it) }
            observer = null
            isListening = false
            Log.i(TAG, "📸 截图无感监听器已停止")
        } catch (e: Exception) {
            Log.w(TAG, "注销截图监听器失败: ${e.message}", e)
        }
    }

    private fun handleMediaChange(context: Context, uri: Uri) {
        val uriStr = uri.toString()
        if (processedUris.contains(uriStr)) return

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val data = if (dataIdx != -1) cursor.getString(dataIdx) ?: "" else ""

                    if (isScreenshot(name, data)) {
                        processedUris.add(uriStr)
                        if (processedUris.size > 200) {
                            processedUris.clear()
                        }
                        ScreenshotOcrProcessor.processScreenshot(context, uri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "查询媒体库截图信息异常: ${e.message}", e)
        }
    }

    private fun handleLatestMedia(context: Context) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    val id = if (idIdx != -1) cursor.getLong(idIdx) else -1L
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val data = if (dataIdx != -1) cursor.getString(dataIdx) ?: "" else ""

                    if (id != -1L && isScreenshot(name, data)) {
                        val itemUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        val uriStr = itemUri.toString()
                        if (!processedUris.contains(uriStr)) {
                            processedUris.add(uriStr)
                            ScreenshotOcrProcessor.processScreenshot(context, itemUri)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "轮询最新截图信息异常: ${e.message}", e)
        }
    }

    fun isScreenshot(name: String, path: String): Boolean {
        val lowerName = name.lowercase()
        val lowerPath = path.lowercase()

        return lowerName.contains("screenshot") ||
               lowerName.contains("截屏") ||
               lowerPath.contains("/screenshots/") ||
               lowerPath.contains("/screenshot/") ||
               lowerPath.contains("/截屏/") ||
               lowerName.startsWith("screen_") ||
               lowerName.startsWith("screenshot_")
    }
}
