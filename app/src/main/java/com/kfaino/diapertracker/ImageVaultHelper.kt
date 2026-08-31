package com.kfaino.diapertracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * 本地沙盒实物照片与发票凭证管理引擎 (Item Photo & Receipt Vault Engine)
 * - 100% 离线私有沙盒存储于 context.filesDir/item_vault/
 * - 自动纠正相机 EXIF 旋转角度
 * - 高质量采样压缩与多级内存 LRU Cache 缓存防 OOM
 */
object ImageVaultHelper {

    private const val VAULT_DIR_NAME = "item_vault"
    private const val MAX_IMAGE_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    // 内存 LRU 缓存 (分配可用内存的 1/8)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /** 获取沙盒私有存储目录 */
    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** 根据文件名获取本地沙盒文件对象 */
    fun getVaultFile(context: Context, filename: String): File? {
        if (filename.isBlank()) return null
        val file = File(getVaultDir(context), filename)
        return if (file.exists()) file else null
    }

    /**
     * 将系统相册/拍照 Uri 复制、纠偏并压缩写入沙盒私有目录
     * @return 沙盒中的纯文件名 (如 photo_uuid.jpg)，失败返回 null
     */
    fun saveUriToVault(context: Context, sourceUri: Uri, prefix: String = "photo"): String? {
        return try {
            // 1. 读取 EXIF 旋转角度
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                try {
                    val exif = ExifInterface(input)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (e: Exception) {
                    Log.w("ImageVaultHelper", "读取 EXIF 旋转属性失败", e)
                }
            }

            // 2. 解码原始位图尺寸
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 3. 计算下采样率
            var sampleSize = 1
            while ((origWidth / sampleSize) > MAX_IMAGE_DIMENSION || (origHeight / sampleSize) > MAX_IMAGE_DIMENSION) {
                sampleSize *= 2
            }

            // 4. 正式加载位图
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            var bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 5. 根据 EXIF 旋转纠正方向
            bitmap = rotateBitmapIfNeeded(bitmap, orientation)

            // 6. 写入私有沙盒文件
            val targetFilename = "${prefix}_${UUID.randomUUID()}.jpg"
            val targetFile = File(getVaultDir(context), targetFilename)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }

            targetFilename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 根据 EXIF 方向旋转位图 */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * 将 Bitmap 直接保存至私有沙盒
     */
    fun saveBitmapToVault(context: Context, bitmap: Bitmap, prefix: String = "photo"): String? {
        return try {
            val filename = "${prefix}_${UUID.randomUUID()}.jpg"
            val outFile = File(getVaultDir(context), filename)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            filename
        } catch (e: Exception) {
            android.util.Log.w("ImageVaultHelper", "保存 Bitmap 失败", e)
            null
        }
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String = "photo"): String? = saveBitmapToVault(context, bitmap, prefix)

    /**
     * 高效按需加载采样缩略图 (带 LRU 内存缓存)
     */
    fun loadSampledBitmap(context: Context, filename: String, reqWidth: Int = 300, reqHeight: Int = 300): Bitmap? {
        if (filename.isBlank()) return null

        val cacheKey = "${filename}_${reqWidth}x${reqHeight}"
        memoryCache.get(cacheKey)?.let { return it }

        val file = getVaultFile(context, filename) ?: return null

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val resultBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            if (resultBitmap != null) {
                memoryCache.put(cacheKey, resultBitmap)
            }
            resultBitmap
        } catch (_: Exception) {
            null
        }
    }

    /** 删除沙盒中的图片文件并清除缓存 */
    fun deleteVaultFile(context: Context, filename: String) {
        if (filename.isBlank()) return
        try {
            val file = File(getVaultDir(context), filename)
            if (file.exists()) {
                file.delete()
            }
            // 清理缓存中以该文件名开头的 key
            val snapshot = memoryCache.snapshot()
            for (k in snapshot.keys) {
                if (k.startsWith(filename)) {
                    memoryCache.remove(k)
                }
            }
        } catch (e: Exception) {
            Log.w("ImageVaultHelper", "从相册沙盒删除图片失败: $filename", e)
        }
    }
}
