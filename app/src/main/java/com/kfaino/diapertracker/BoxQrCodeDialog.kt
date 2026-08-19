package com.kfaino.diapertracker

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kfaino.diapertracker.databinding.DialogBoxQrcodeBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BoxQrCodeDialog {

    /**
     * 弹出收纳箱/房间专属二维码标签生成与导出弹窗
     */
    fun show(
        activity: Activity,
        store: DataStore,
        houseName: String,
        roomName: String
    ) {
        val binding = DialogBoxQrcodeBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 统计该空间/房间下的在库物品数
        val itemsInRoom = store.loadAll().filter {
            !it.isRetired && (it.roomName == roomName || it.location.contains(roomName))
        }

        binding.tvQrBoxName.text = "📦 $roomName"
        binding.tvQrBoxSubtitle.text = "🏠 $houseName · 共 ${itemsInRoom.size} 种在库物品 (${itemsInRoom.sumOf { it.qty }} 件)"

        // 构造二维码数据协议 (如 collecter://room?house=我的家&room=主卧)
        val qrContent = "collecter://room?house=${Uri.encode(houseName)}&room=${Uri.encode(roomName)}"

        // 离线生成 QR Code Bitmap
        val qrBitmap = generateQrCodeBitmap(qrContent, 600, 600)
        if (qrBitmap != null) {
            binding.ivQrCode.setImageBitmap(qrBitmap)
        }

        binding.btnCloseQr.applyPressScaleAnimation(0.90f)
        binding.btnCloseQr.setOnClickListener { dialog.dismiss() }

        binding.btnSaveQrToGallery.applyPressScaleAnimation(0.94f)
        binding.btnSaveQrToGallery.setOnClickListener {
            val labelBitmap = captureViewBitmap(binding.cardQrLabelPreview)
            if (labelBitmap != null) {
                val success = saveBitmapToGallery(activity, labelBitmap, "Collecter_Label_${roomName}")
                if (success) {
                    Toast.makeText(activity, "已保存二维码标签到系统相册！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "保存失败，请检查相册权限", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnShareQrLabel.applyPressScaleAnimation(0.94f)
        binding.btnShareQrLabel.setOnClickListener {
            val labelBitmap = captureViewBitmap(binding.cardQrLabelPreview)
            if (labelBitmap != null) {
                val cacheFile = File(activity.cacheDir, "收纳标签_${roomName}.png")
                try {
                    FileOutputStream(cacheFile).use { out ->
                        labelBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    ExportManager.shareFile(activity, cacheFile, title = "分享收纳标签", mimeType = "image/png")
                } catch (_: Exception) {
                    Toast.makeText(activity, "分享失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    /** 生成 ZXing 二维码位图 */
    private fun generateQrCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.parseColor("#0F172A") else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 将 View 完整渲染为 Bitmap */
    private fun captureViewBitmap(view: android.view.View): Bitmap? {
        return try {
            val w = if (view.width > 0) view.width else 720
            val h = if (view.height > 0) view.height else 880
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /** 保存位图至系统相册 (兼容 Android 10+ MediaStore) */
    private fun saveBitmapToGallery(activity: Activity, bitmap: Bitmap, title: String): Boolean {
        return try {
            val filename = "${title}_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Collecter")
                }
                val imageUri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = activity.contentResolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Collecter"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val imageFile = File(imagesDir, filename)
                fos = FileOutputStream(imageFile)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
