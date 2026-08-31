package com.kfaino.diapertracker

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * 🪪 家庭证照安全防盗流水印导出引擎 (Identity Watermark Helper)
 * - 纯本地离线为证件正反面扫描件压印 45° 倾斜防盗流水印
 * - 防止证件在网络外传或打印时被恶意挪用
 */
object IdentityWatermarkHelper {

    /** 为指定 Bitmap 压印倾斜防盗流水印 */
    fun applySecurityWatermark(
        source: Bitmap,
        watermarkText: String = "仅供办理业务使用 · 他用无效"
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 绘制原图
        canvas.drawBitmap(source, 0f, 0f, null)

        // 准备水印 Paint
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            alpha = 75
            textSize = (width / 22f).coerceAtLeast(32f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // 倾斜 45 度网格平铺绘制水印
        canvas.save()
        canvas.rotate(-30f, width / 2f, height / 2f)

        val stepX = width / 2.5f
        val stepY = height / 4f

        val startX = -width * 0.8f
        val endX = width * 1.8f
        val startY = -height * 0.8f
        val endY = height * 1.8f

        var curY = startY
        while (curY < endY) {
            var curX = startX
            while (curX < endX) {
                canvas.drawText(watermarkText, curX, curY, paint)
                curX += stepX
            }
            curY += stepY
        }

        canvas.restore()
        return output
    }

    /** 保存带水印证照至系统相册 (Pictures/Collecter) */
    fun saveWatermarkedImage(context: Context, bitmap: Bitmap, title: String): Uri? {
        val fileName = "Protected_${title}_${System.currentTimeMillis()}.jpg"
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Collecter")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } else {
                val imagesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Collecter")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val imageFile = File(imagesDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                uri = Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }
}
