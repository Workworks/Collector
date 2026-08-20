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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🏷️ 智能收纳便签工坊 (Label & Sticky Note Studio)
 * 支持 5 大高颜值多模态收纳便签：
 * 1. 箱盒全景清单便签
 * 2. 食材生鲜开封保鲜便签
 * 3. 药箱常备药对症用法便签
 * 4. 数码线缆与设备规格便签
 * 5. 重要随身资产防丢联系便签
 */
object BoxQrCodeDialog {

    private const val TAG = "BoxQrCodeDialog"

    fun show(
        activity: Activity,
        store: DataStore,
        houseName: String,
        roomName: String,
        defaultTemplate: Int = BluetoothPrinterHelper.TPL_BOX
    ) {
        val binding = DialogBoxQrcodeBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 加载当前空间/房间下的物品与全量候选数据
        val allEntries = store.loadAll().filter { !it.isRetired }
        val itemsInRoom = allEntries.filter { it.roomName == roomName || it.location.contains(roomName) }

        var currentTemplate = defaultTemplate
        var currentQrBitmap: Bitmap? = null
        var currentTitle = ""
        var currentSubtitle = ""
        var currentAttrLines = listOf<String>()

        fun renderTemplate(tpl: Int) {
            currentTemplate = tpl
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            when (tpl) {
                BluetoothPrinterHelper.TPL_FOOD -> {
                    val foodItem = itemsInRoom.firstOrNull { it.category == "零食" || it.category == "生鲜" || it.category == "食品" }
                        ?: allEntries.firstOrNull { it.category == "零食" || it.category == "生鲜" }
                    val name = foodItem?.brand ?: "生鲜食材分装盒"
                    currentTitle = "🥫 $name"
                    currentSubtitle = "❄️ 冰箱冷冻/冷藏区 · 开封保鲜标记"
                    currentAttrLines = listOf(
                        "• 分装日期：$todayStr (开封标记)",
                        "• 建议消灭：3天内尽快食用 / 密封冷冻",
                        "• 存放空间：$houseName · $roomName"
                    )
                    binding.tvLabelFooter.text = "🥦 鲜度监测 · 遵循先进先出 清库存优先"
                    val qrUrl = "collecter://food?name=${Uri.encode(name)}&house=${Uri.encode(houseName)}"
                    currentQrBitmap = generateQrCodeBitmap(qrUrl, 500, 500)
                }
                BluetoothPrinterHelper.TPL_MEDICINE -> {
                    val medItem = itemsInRoom.firstOrNull { it.category == "药品" || it.category == "保健" || it.category == "医疗" }
                        ?: allEntries.firstOrNull { it.category == "药品" }
                    val name = medItem?.brand ?: "家庭常备急救药盒"
                    currentTitle = "💊 $name"
                    currentSubtitle = "🩺 对症速查 · 用法用量指示"
                    currentAttrLines = listOf(
                        "• 对症类型：常用应急 / 感冒退热 / 外伤消炎",
                        "• 用法用量：一日2~3次，温水送服 (遵医嘱)",
                        "• 有效期限：开封后建议6个月内使用完毕"
                    )
                    binding.tvLabelFooter.text = "⚠️ 谨遵医嘱 · 过期变质药品严禁服用"
                    val qrUrl = "collecter://medicine?name=${Uri.encode(name)}&house=${Uri.encode(houseName)}"
                    currentQrBitmap = generateQrCodeBitmap(qrUrl, 500, 500)
                }
                BluetoothPrinterHelper.TPL_CABLE -> {
                    val cableItem = itemsInRoom.firstOrNull { it.category == "数码" || it.category == "耗材" }
                        ?: allEntries.firstOrNull { it.category == "数码" }
                    val name = cableItem?.brand ?: "Type-C 100W 编织快充线"
                    currentTitle = "🔌 $name"
                    currentSubtitle = "⚡ 硬件线缆与适配配件标识"
                    currentAttrLines = listOf(
                        "• 适配设备：笔记本电脑 / 平板 / 手机 / 移动电源",
                        "• 额定规格：100W PD 20V/5A 快充标准",
                        "• 存放位置：$houseName · $roomName"
                    )
                    binding.tvLabelFooter.text = "🏷️ 线缆防混淆 · 专线专用 避免过载发热"
                    val qrUrl = "collecter://cable?name=${Uri.encode(name)}"
                    currentQrBitmap = generateQrCodeBitmap(qrUrl, 500, 500)
                }
                BluetoothPrinterHelper.TPL_ANTI_LOST -> {
                    val item = itemsInRoom.firstOrNull() ?: allEntries.firstOrNull()
                    val name = item?.brand ?: "重要随身物品"
                    currentTitle = "🪪 $name"
                    currentSubtitle = "🛡️ 个人专属归属物 · 拾获请联系"
                    currentAttrLines = listOf(
                        "• 物品归属：Collecter 用户私有资产",
                        "• 物主留言：若不慎遗失，恳请扫码或联系归还",
                        "• 酬谢声明：万分感谢您的善举，必有重谢！"
                    )
                    binding.tvLabelFooter.text = "✨ 扫码安全联络物主 · 感谢您的善意举动"
                    val qrUrl = "collecter://lost?item=${Uri.encode(name)}"
                    currentQrBitmap = generateQrCodeBitmap(qrUrl, 500, 500)
                }
                else -> { // TPL_BOX
                    currentTitle = "📦 $roomName"
                    currentSubtitle = "🏠 $houseName · 共 ${itemsInRoom.size} 种在库物品 (${itemsInRoom.sumOf { it.qty }} 件)"
                    val totalVal = itemsInRoom.sumOf { it.price * it.qty }
                    val mainItems = itemsInRoom.take(3).joinToString("、") { "${it.brand}×${it.qty}" }
                    currentAttrLines = listOf(
                        "• 在库明细：${itemsInRoom.size} 种物品 (总估值 ¥${String.format(Locale.getDefault(), "%.2f", totalVal)})",
                        "• 核心物品：${if (mainItems.isNotBlank()) mainItems else "暂无登记明细"}",
                        "• 空间位置：$houseName / $roomName"
                    )
                    binding.tvLabelFooter.text = "📱 使用 Collecter 扫一扫 · 秒查箱内明细与快速盘点"
                    val qrUrl = "collecter://room?house=${Uri.encode(houseName)}&room=${Uri.encode(roomName)}"
                    currentQrBitmap = generateQrCodeBitmap(qrUrl, 500, 500)
                }
            }

            binding.tvQrBoxName.text = currentTitle
            binding.tvQrBoxSubtitle.text = currentSubtitle
            binding.tvAttrLine1.text = currentAttrLines.getOrNull(0) ?: ""
            binding.tvAttrLine2.text = currentAttrLines.getOrNull(1) ?: ""
            binding.tvAttrLine3.text = currentAttrLines.getOrNull(2) ?: ""

            if (currentQrBitmap != null) {
                binding.ivQrCode.setImageBitmap(currentQrBitmap)
            }
        }

        // 初始化模板选择
        when (defaultTemplate) {
            BluetoothPrinterHelper.TPL_FOOD -> binding.chipTplFood.isChecked = true
            BluetoothPrinterHelper.TPL_MEDICINE -> binding.chipTplMedicine.isChecked = true
            BluetoothPrinterHelper.TPL_CABLE -> binding.chipTplCable.isChecked = true
            BluetoothPrinterHelper.TPL_ANTI_LOST -> binding.chipTplLost.isChecked = true
            else -> binding.chipTplBox.isChecked = true
        }
        renderTemplate(defaultTemplate)

        // 切换 Chips 响应
        binding.chipGroupTemplates.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            when (id) {
                R.id.chip_tpl_food -> renderTemplate(BluetoothPrinterHelper.TPL_FOOD)
                R.id.chip_tpl_medicine -> renderTemplate(BluetoothPrinterHelper.TPL_MEDICINE)
                R.id.chip_tpl_cable -> renderTemplate(BluetoothPrinterHelper.TPL_CABLE)
                R.id.chip_tpl_lost -> renderTemplate(BluetoothPrinterHelper.TPL_ANTI_LOST)
                else -> renderTemplate(BluetoothPrinterHelper.TPL_BOX)
            }
        }

        binding.btnCloseQr.applyPressScaleAnimation(0.90f)
        binding.btnCloseQr.setOnClickListener { dialog.dismiss() }

        binding.btnBluetoothPrint.applyPressScaleAnimation(0.94f)
        binding.btnBluetoothPrint.setOnClickListener {
            BluetoothPrinterHelper.printLabel(
                activity = activity,
                templateType = currentTemplate,
                title = currentTitle,
                subtitle = currentSubtitle,
                attrLines = currentAttrLines,
                items = itemsInRoom,
                qrBitmap = currentQrBitmap
            )
        }

        binding.btnNfcWrite.applyPressScaleAnimation(0.94f)
        binding.btnNfcWrite.setOnClickListener {
            NfcHelper.prepareWriteBoxTag(activity, houseName, roomName)
        }

        binding.btnSaveQrToGallery.applyPressScaleAnimation(0.94f)
        binding.btnSaveQrToGallery.setOnClickListener {
            val labelBitmap = captureViewBitmap(binding.cardQrLabelPreview)
            if (labelBitmap != null) {
                val cleanTitle = currentTitle.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
                val success = saveBitmapToGallery(activity, labelBitmap, "Collecter_Label_${cleanTitle}")
                if (success) {
                    Toast.makeText(activity, "🎉 已保存高质感便签卡片到系统相册！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "保存失败，请检查相册权限", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnShareQrLabel.applyPressScaleAnimation(0.94f)
        binding.btnShareQrLabel.setOnClickListener {
            val labelBitmap = captureViewBitmap(binding.cardQrLabelPreview)
            if (labelBitmap != null) {
                val cacheFile = File(activity.cacheDir, "收纳便签_${System.currentTimeMillis()}.png")
                try {
                    FileOutputStream(cacheFile).use { out ->
                        labelBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    ExportManager.shareFile(activity, cacheFile, title = "分享收纳便签", mimeType = "image/png")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "分享收纳便签失败", e)
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
            android.util.Log.w(TAG, "生成二维码位图失败: $content", e)
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
        } catch (e: Exception) {
            android.util.Log.w(TAG, "渲染便签 View 位图失败", e)
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
            android.util.Log.w(TAG, "保存图片至相册失败", e)
            false
        }
    }
}
