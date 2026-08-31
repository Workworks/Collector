package com.kfaino.diapertracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executors

/**
 * 🧠 截图无感离线 OCR 提取与智能打标处理器 (Screenshot OCR Processor)
 * - 纯本地离线端侧 ML Kit 提取截图中的全文文字
 * - 智能提取核心标题、智能打标（电商订单/技术资料/美食食谱/聊天备忘等）
 * - 自动归档至私有媒体沙盒与 ClippingVault
 */
object ScreenshotOcrProcessor {

    private const val TAG = "ScreenshotOcrProcessor"
    private val executor = Executors.newSingleThreadExecutor()
    private val recognizer by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }

    fun processScreenshot(
        context: Context,
        imageUri: Uri,
        onComplete: ((ClippingRecord?) -> Unit)? = null
    ) {
        val store = DataStore(context.applicationContext)
        if (!store.isScreenshotCaptureEnabled()) {
            onComplete?.invoke(null)
            return
        }

        executor.execute {
            var inputStream: InputStream? = null
            try {
                val cr = context.contentResolver
                inputStream = cr.openInputStream(imageUri)
                if (inputStream == null) {
                    Log.w(TAG, "无法打开截图 Uri: $imageUri")
                    onComplete?.invoke(null)
                    return@execute
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.w(TAG, "解码截图失败: $imageUri")
                    onComplete?.invoke(null)
                    return@execute
                }

                // 1. 将原图保存到应用私有沙盒中（防止系统清理相册导致原图丢失）
                val savedFilename = ImageVaultHelper.saveBitmap(context, bitmap) ?: ""
                val imageList = if (savedFilename.isNotBlank()) listOf(savedFilename) else emptyList()

                // 2. 端侧 ML Kit 离线 OCR 提取文字
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text.trim()
                        val analyzed = analyzeScreenshotText(rawText)

                        val record = ClippingRecord(
                            id = UUID.randomUUID().toString(),
                            title = analyzed.title,
                            originalUrl = "",
                            sourcePlatform = "screenshot",
                            fullMarkdown = "",
                            ocrRawText = rawText,
                            localImagePaths = imageList,
                            summary = analyzed.summary,
                            tags = analyzed.tags,
                            capturedAt = System.currentTimeMillis()
                        )

                        store.addOrUpdateClipping(record)
                        Log.i(TAG, "📸 成功收纳截图并完成 OCR 索引: ${record.title}")

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context.applicationContext,
                                "📸 已自动收纳截图「${record.title.take(15)}」并完成 OCR 索引",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onComplete?.invoke(record)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "截图 OCR 识别失败", e)
                        onComplete?.invoke(null)
                    }
            } catch (e: Exception) {
                Log.w(TAG, "处理截图异常: ${e.message}", e)
                onComplete?.invoke(null)
            } finally {
                try {
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "关闭截图流失败", e)
                }
            }
        }
    }

    data class AnalyzedResult(
        val title: String,
        val summary: String,
        val tags: List<String>
    )

    /** 纯文本分析与智能打标 (纯函数，便于单测) */
    fun analyzeScreenshotText(text: String): AnalyzedResult {
        if (text.isBlank()) {
            return AnalyzedResult(
                title = "截图快照",
                summary = "（无文字内容）",
                tags = listOf("截图快照")
            )
        }

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = lines.firstOrNull { it.length in 2..35 } ?: lines.firstOrNull()?.take(30) ?: "截图快照"
        val summary = if (text.length > 80) text.replace("\n", " ").take(80) + "..." else text.replace("\n", " ")

        val suggestedTags = mutableListOf<String>()
        suggestedTags.add("截图快照")

        val lower = text.lowercase()
        if (lower.contains("¥") || lower.contains("元") || lower.contains("实付款") || lower.contains("订单") || lower.contains("购物车")) {
            suggestedTags.add("电商订单")
        }
        if (lower.contains("代码") || lower.contains("function") || lower.contains("class ") || lower.contains("val ") || lower.contains("import ")) {
            suggestedTags.add("技术资料")
        }
        if (lower.contains("配料") || lower.contains("做法") || lower.contains("调味") || lower.contains("煮") || lower.contains("炒")) {
            suggestedTags.add("美食食谱")
        }
        if (lower.contains("微信") || lower.contains("聊天记录") || lower.contains("撤回")) {
            suggestedTags.add("聊天备忘")
        }
        if (lower.contains("知乎") || lower.contains("公众号") || lower.contains("文章") || lower.contains("阅读")) {
            suggestedTags.add("精选文章")
        }

        return AnalyzedResult(
            title = title,
            summary = summary,
            tags = suggestedTags.distinct()
        )
    }
}
