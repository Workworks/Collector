package com.kfaino.diapertracker

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎨 物品时光胶囊与生活回忆录海报生成引擎 (Life Capsule Poster Generator)
 * - 纯原生 Canvas 极速离线绘制 1080P 拍立得/生活杂志风长图画册
 * - 支持物品高光实拍、拥有天数、真香指数、时光里程碑故事流排版
 * - 一键导出保存至系统相册或分享
 */
object LifeCapsulePosterGenerator {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /** 生成时光胶囊高清长图海报 Bitmap */
    fun generatePosterBitmap(context: Context, entry: Entry): Bitmap {
        val width = 1080
        val padding = 64f
        val contentWidth = width - padding * 2

        // 计算动态海报总高度
        var estHeight = 1100f // 基础头部、封面卡片与底部高度
        for (m in entry.memoryMoments) {
            estHeight += 180f
            if (m.story.isNotBlank()) {
                val lines = (m.story.length / 24) + 1
                estHeight += lines * 36f
            }
            if (m.photoPath.isNotBlank()) {
                estHeight += 420f
            }
        }
        val totalHeight = estHeight.toInt().coerceAtLeast(1400)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 绘制背景 (深邃星空暗夜渐变)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, totalHeight.toFloat(),
                intArrayOf(
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#1E293B"),
                    Color.parseColor("#090D16")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        // 2. 顶部装饰光效与标题栏
        val decoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            alpha = 35
        }
        canvas.drawCircle(width * 0.85f, 120f, 260f, decoPaint)

        var curY = 80f

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 24f
            isFakeBoldText = true
            letterSpacing = 0.15f
        }
        canvas.drawText("COLLECTER · LIFE CAPSULE JOURNAL", padding, curY, brandPaint)
        curY += 45f

        val mainTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            isFakeBoldText = true
        }
        canvas.drawText(entry.brand, padding, curY, mainTitlePaint)
        curY += 40f

        val subInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
        }
        val typeBadge = if (entry.isDigital) "📷 ${entry.getDigitalTypeDisplayName()}" else "🏷️ ${entry.category}"
        val daysText = "⏳ 陪伴 ${entry.getDaysOwned()} 天 · 累计日均 ￥${String.format(Locale.getDefault(), "%.2f", entry.getDailyCost())}/天"
        canvas.drawText("$typeBadge  |  $daysText", padding, curY, subInfoPaint)
        curY += 45f

        // 3. 绘制封面主图卡片 (如有实物照片)
        if (entry.photoPath.isNotBlank()) {
            val photoFile = File(context.filesDir, "item_photos/${entry.photoPath}")
            val realFile = if (photoFile.exists()) photoFile else File(entry.photoPath)
            if (realFile.exists()) {
                val coverBmp = decodeSampledBitmap(realFile.absolutePath, contentWidth.toInt(), 540)
                if (coverBmp != null) {
                    val coverRect = RectF(padding, curY, padding + contentWidth, curY + 520f)
                    drawRoundedBitmap(canvas, coverBmp, coverRect, 28f)
                    curY += 560f
                }
            }
        }

        // 4. 统计与心动指数徽章卡片
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val badgeRect = RectF(padding, curY, padding + contentWidth, curY + 110f)
        canvas.drawRoundRect(badgeRect, 22f, 22f, cardPaint)
        canvas.drawRoundRect(badgeRect, 22f, 22f, cardStroke)

        val statTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 30f
            isFakeBoldText = true
        }
        val stars = "★".repeat(entry.getAverageRating().toInt().coerceIn(1, 5))
        canvas.drawText("真香指数: $stars (${String.format(Locale.getDefault(), "%.1f", entry.getAverageRating())}分)", padding + 32f, curY + 68f, statTextPaint)

        val momentCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("🎞️ 记录了 ${entry.memoryMoments.size} 个高光瞬间", width - padding - 32f, curY + 68f, momentCountPaint)
        curY += 150f

        // 5. 时光回忆轴标题
        val timelineTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 34f
            isFakeBoldText = true
        }
        canvas.drawText("✨ 生活高光回忆轴 (Moments Timeline)", padding, curY, timelineTitlePaint)
        curY += 40f

        // 6. 逐条绘制时光回忆里程碑
        if (entry.memoryMoments.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 28f
            }
            canvas.drawText("暂未记录生活回忆，快去添加第一个故事吧~", padding, curY + 40f, emptyPaint)
            curY += 120f
        } else {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                strokeWidth = 3f
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10B981")
            }

            for ((idx, moment) in entry.memoryMoments.withIndex()) {
                val startX = padding + 24f
                val contentX = padding + 70f
                val contentW = width - padding - contentX

                // 节点圆点
                canvas.drawCircle(startX, curY + 20f, 10f, dotPaint)

                // 节点标题与表情
                val momentTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 32f
                    isFakeBoldText = true
                }
                val mTitle = "${moment.moodEmoji} ${moment.title.ifBlank { "高光回忆 #${idx + 1}" }}"
                canvas.drawText(mTitle, contentX, curY + 28f, momentTitlePaint)

                // 日期与评分
                val mDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#64748B")
                    textSize = 24f
                }
                val mDateStr = dateFormat.format(Date(moment.date)) + " · ${"★".repeat(moment.rating)}"
                canvas.drawText(mDateStr, contentX, curY + 65f, mDatePaint)
                curY += 95f

                // 文字故事 (StaticLayout 自动折行)
                if (moment.story.isNotBlank()) {
                    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#CBD5E1")
                        textSize = 28f
                    }
                    val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder.obtain(moment.story, 0, moment.story.length, textPaint, contentW.toInt())
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(8f, 1.15f)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(moment.story, textPaint, contentW.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.15f, 8f, false)
                    }

                    canvas.save()
                    canvas.translate(contentX, curY)
                    staticLayout.draw(canvas)
                    canvas.restore()
                    curY += staticLayout.height + 25f
                }

                // 照片插图
                if (moment.photoPath.isNotBlank()) {
                    val pFile = File(context.filesDir, "item_photos/${moment.photoPath}")
                    val realPFile = if (pFile.exists()) pFile else File(moment.photoPath)
                    if (realPFile.exists()) {
                        val mBmp = decodeSampledBitmap(realPFile.absolutePath, contentW.toInt(), 380)
                        if (mBmp != null) {
                            val pRect = RectF(contentX, curY, contentX + contentW, curY + 360f)
                            drawRoundedBitmap(canvas, mBmp, pRect, 20f)
                            curY += 390f
                        }
                    }
                }

                curY += 30f
            }
        }

        // 7. 底部水印与时间徽章
        curY = (totalHeight - 120f).coerceAtLeast(curY + 40f)
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            strokeWidth = 2f
        }
        canvas.drawLine(padding, curY, width - padding, curY, dividerPaint)
        curY += 45f

        val footerBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
        }
        val footerDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Collecter 智能资产与数码时光胶囊 · 珍藏每一件美好", padding, curY, footerBrandPaint)
        canvas.drawText(timeFormat.format(Date()), width - padding, curY, footerDatePaint)

        return bitmap
    }

    /** 绘制圆角矩形图片 */
    private fun drawRoundedBitmap(canvas: Canvas, bitmap: Bitmap, rect: RectF, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // 居中裁剪缩放矩阵
        val matrix = Matrix()
        val scale = Math.max(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val dx = rect.left + (rect.width() - bitmap.width * scale) * 0.5f
        val dy = rect.top + (rect.height() - bitmap.height * scale) * 0.5f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        shader.setLocalMatrix(matrix)

        paint.shader = shader
        canvas.drawRoundRect(rect, radius, radius, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#334155")
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    /** 采样解码大图以防 OOM */
    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            BitmapFactory.decodeFile(filePath, options)
        } catch (_: Exception) {
            null
        }
    }

    /** 保存海报至系统相册并返回存储路径 */
    fun savePosterToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val fileName = "Collecter_Capsule_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Collecter")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        try {
            val out: OutputStream? = resolver.openOutputStream(uri)
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
                out.close()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
