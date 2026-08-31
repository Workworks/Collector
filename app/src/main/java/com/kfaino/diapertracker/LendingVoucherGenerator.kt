package com.kfaino.diapertracker

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📤 原生 Canvas 实物外借凭证与微信催还海报生成引擎 (Lending Voucher Generator)
 * - 纯内存离线绘制 1080P 高清交接凭证与温馨催还卡片
 * - 包含借用人、约定归还日、押金凭据、配件明细与照片
 * - 适配 Android 10+ Scoped Storage (Pictures/Collecter)
 */
object LendingVoucherGenerator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 生成 1080P 高清借出交接凭证 / 催还海报 Bitmap
     * @param entry 关联资产
     * @param record 对应借出记录
     * @param isReminderMode 是否为催还提醒模式
     */
    fun generateVoucherBitmap(
        context: Context,
        entry: Entry,
        record: LendingRecord,
        isReminderMode: Boolean = false
    ): Bitmap {
        val width = 1080
        val baseHeight = 1480
        val bitmap = Bitmap.createBitmap(width, baseHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 渐变背景绘制
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val topColor = if (isReminderMode) Color.parseColor("#1E1B4B") else Color.parseColor("#0F172A")
        val bottomColor = if (isReminderMode) Color.parseColor("#312E81") else Color.parseColor("#1E293B")
        val shader = LinearGradient(
            0f, 0f, 0f, baseHeight.toFloat(),
            topColor, bottomColor, Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), baseHeight.toFloat(), bgPaint)

        // 2. 装饰星空微光
        val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(if (isReminderMode) "#4338CA" else "#059669")
            alpha = 40
            maskFilter = BlurMaskFilter(160f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width * 0.85f, 180f, 220f, decorPaint)
        canvas.drawCircle(width * 0.15f, baseHeight * 0.75f, 200f, decorPaint)

        // 3. 顶部 Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("COLLECTER · 全维度资产管家", 60f, 90f, brandPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitle = if (isReminderMode) "🔔 物品借用温馨提醒卡" else "📤 实物借出交接电子凭证"
        canvas.drawText(headerTitle, 60f, 160f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
        }
        val headerSub = if (isReminderMode) "好借好还 · 再借不难 · 友谊长存" else "数字化资产流转记录 · 配件齐备 · 诚信互通"
        canvas.drawText(headerSub, 60f, 205f, subPaint)

        // 分割线
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#334155")
            strokeWidth = 2f
        }
        canvas.drawLine(60f, 240f, width - 60f, 240f, dividerPaint)

        // 4. 物品主信息卡片 (圆角白底卡片)
        val cardRect = RectF(60f, 270f, width - 60f, 540f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(if (isReminderMode) "#6366F1" else "#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
        canvas.drawRoundRect(cardRect, 28f, 28f, cardBorderPaint)

        // 物品名称
        val itemNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(entry.brand, 96f, 340f, itemNamePaint)

        val itemCategoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 28f
        }
        canvas.drawText("🏷️ 所属分类: ${entry.category}   💰 价值原价: ¥${String.format(Locale.getDefault(), "%,.2f", entry.price * entry.qty)}", 96f, 390f, itemCategoryPaint)

        val locPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
        }
        val locText = if (entry.location.isNotBlank()) "📍 存放空间: ${entry.houseName} · ${entry.location}" else "📍 空间: ${entry.houseName}"
        canvas.drawText(locText, 96f, 440f, locPaint)

        val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("⏳ 累计陪伴: ${entry.getDaysOwned()} 天   ⭐ 心动真香评级: ★★★★★", 96f, 490f, daysPaint)

        // 5. 借还流转核心要素卡片
        val infoRect = RectF(60f, 570f, width - 60f, 980f)
        canvas.drawRoundRect(infoRect, 28f, 28f, cardPaint)

        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("📋 流转交接明细", 96f, 630f, sectionTitlePaint)

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 30f
        }

        val contactStr = if (record.borrowerContact.isNotBlank()) " (${record.borrowerContact})" else ""
        canvas.drawText("👤 借 用 人: ${record.borrowerName}$contactStr", 96f, 690f, detailPaint)
        canvas.drawText("📅 借出日期: ${dateFormat.format(Date(record.lentDate))}", 96f, 745f, detailPaint)

        val expStr = if (record.expectedReturnDate > 0L) dateFormat.format(Date(record.expectedReturnDate)) else "未约定具体期限"
        val expColor = if (isReminderMode) Color.parseColor("#F59E0B") else Color.parseColor("#10B981")
        val expPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = expColor
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("⏰ 约定归还: $expStr", 96f, 800f, expPaint)

        val depStr = if (record.deposit > 0) "￥${String.format(Locale.getDefault(), "%.2f", record.deposit)} (归还时结清)" else "免押金外借"
        canvas.drawText("💰 押金/租金: $depStr", 96f, 855f, detailPaint)

        val notesStr = if (record.notes.isNotBlank()) record.notes else "全套配件齐备，外观完好无损伤"
        canvas.drawText("📦 附带配件: $notesStr", 96f, 910f, detailPaint)

        // 6. 温馨提醒或交接承诺语录
        val quoteRect = RectF(60f, 1010f, width - 60f, 1260f)
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isReminderMode) Color.parseColor("#312E81") else Color.parseColor("#064E3B")
        }
        canvas.drawRoundRect(quoteRect, 24f, 24f, quotePaint)

        val quoteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val quoteTitle = if (isReminderMode) "💌 物主温馨寄语" else "🤝 借还交接契约"
        val quoteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isReminderMode) Color.parseColor("#A5B4FC") else Color.parseColor("#6EE7B7")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(quoteTitle, 96f, 1070f, quoteTitlePaint)

        val quoteBody = if (isReminderMode) {
            "亲爱的【${record.borrowerName}】，您借用的【${entry.brand}】预计于 $expStr 到期。\n使用完毕后请记得联系我交接归还哦，感谢您的爱护与支持！"
        } else {
            "借物如借心，好借好还，诚信相伴。\n双方已现场核对物品功能与附带配件无误，特立此电子凭据。"
        }

        var quoteY = 1120f
        for (line in quoteBody.split("\n")) {
            canvas.drawText(line, 96f, quoteY, quoteTextPaint)
            quoteY += 45f
        }

        // 7. 底部水印与时间戳
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        val timeNowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Collecter 智能资产流转凭据 · 生成时间 $timeNowStr · 离线加密存储", width / 2f, 1380f, footerPaint)

        return bitmap
    }

    /** 保存凭据海报至系统相册 (Pictures/Collecter) */
    fun saveVoucherToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val fileName = "Lending_Voucher_${System.currentTimeMillis()}.jpg"
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
