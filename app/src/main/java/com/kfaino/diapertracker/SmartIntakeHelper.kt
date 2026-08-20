package com.kfaino.diapertracker

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * AI 多模态智能免录与结构化解析引擎
 * 1. 拍照购物小票 / 发票 / 包装盒 OCR 识别与字段提取
 * 2. 自然语言一句话记账文本智能拆解
 */
object SmartIntakeHelper {

    data class ParsedItem(
        val brand: String = "",
        val category: String = "日用品",
        val price: Double = 0.0,
        val qty: Int = 1,
        val unit: String = "件",
        val purchaseDate: Long = System.currentTimeMillis(),
        val mfgDate: Long = 0L,
        val expDate: Long = 0L,
        val assetType: String = "consumable",
        val notes: String = ""
    )

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /** 识别图片中文字并结构化提取为资产对象 */
    fun parseImageOcr(
        context: Context,
        bitmap: Bitmap,
        onSuccess: (ParsedItem) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val parsed = parseRawText(fullText)
                    onSuccess(parsed)
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "OCR 识别失败")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "处理图像异常")
        }
    }

    /** 自然语言一句话直接解析（例如："山姆买了2箱脱脂牛奶单价65保质期到2026-10-15"） */
    fun parseNaturalLanguage(text: String): ParsedItem {
        return parseRawText(text)
    }

    /** 检测文本是否包含淘宝/京东/拼多多等电商订单或商品分享特征 */
    fun isEcommerceContent(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("tb.cn") || t.contains("taobao.com") || t.contains("tmall.com") ||
               t.contains("jd.com") || t.contains("yangkeduo.com") || t.contains("pinduoduo") ||
               t.contains("￥") || t.contains("【淘宝】") || t.contains("【京东】") ||
               t.contains("实付款") || t.contains("订单编号") || t.contains("已发货")
    }

    private fun parseRawText(raw: String): ParsedItem {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val flatText = raw.replace("\n", " ").trim()

        var brand = ""
        var category = guessCategory(flatText)
        var price = extractPrice(flatText)
        var qty = extractQty(flatText)
        var unit = extractUnit(flatText)
        var purchaseDate = extractDate(flatText, listOf("购买", "下单", "日期", "交易", "开票", "创建时间"))
        var mfgDate = extractDate(flatText, listOf("生产", "制造", "出厂", "mfg"))
        var expDate = extractDate(flatText, listOf("保质期至", "到期", "有效", "截止", "exp", "最佳食用"))
        var assetType = "consumable"
        var specs = ""

        // 提取规格属性 (例如: 黑色 256GB / 颜色: 钛金灰 / 尺码: L)
        val specRegex = Pattern.compile("(?:规格|颜色|尺码|型号|分类|版本)[：:]\\s*([^\\s,，\n]+)")
        val specMatcher = specRegex.matcher(raw)
        if (specMatcher.find()) {
            specs = specMatcher.group(1)?.trim().orEmpty()
        }

        // 提取名称 (通常在小票/订单的前几行，去除电商与票据无用干扰词)
        if (lines.isNotEmpty()) {
            val candidate = lines.firstOrNull { l ->
                !l.contains("小票") && !l.contains("发票") && !l.contains("收银") &&
                !l.contains("欢迎光临") && !l.contains("总计") && !l.contains("金额") &&
                !l.contains("实付款") && !l.contains("订单编号") && !l.contains("已发货") &&
                !l.contains("退款") && !l.contains("售后") && !l.contains("旗舰店") &&
                !l.contains("专卖店") && !l.contains("运费险") && !l.contains("店铺合计") &&
                !l.contains("查看物流") && !l.contains("再次购买") && !l.contains("去评价") &&
                l.length in 2..30
            } ?: lines.first()
            brand = cleanBrandName(candidate)
        }

        if (brand.isBlank()) {
            brand = cleanBrandName(flatText.take(25))
        }

        // 推断类型
        if (expDate > 0L) {
            assetType = "expiring"
        } else if (category == "数码" || price > 500) {
            assetType = "depreciating"
        }

        val noteText = if (specs.isNotBlank()) "规格: $specs (由电商订单自动提取)" else "由 AI 智能提取"

        return ParsedItem(
            brand = brand.ifBlank { "物品" },
            category = category,
            price = price,
            qty = qty.coerceAtLeast(1),
            unit = unit,
            purchaseDate = if (purchaseDate > 0) purchaseDate else System.currentTimeMillis(),
            mfgDate = mfgDate,
            expDate = expDate,
            assetType = assetType,
            notes = noteText
        )
    }

    private fun guessCategory(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("手机") || t.contains("电脑") || t.contains("耳机") || t.contains("相机") ||
            t.contains("键盘") || t.contains("充电") || t.contains("数码") || t.contains("显卡") ||
            t.contains("显示器") || t.contains("ipad") || t.contains("iphone") || t.contains("mac") -> "数码"

            t.contains("奶") || t.contains("零食") || t.contains("面包") || t.contains("咖啡") ||
            t.contains("水果") || t.contains("肉") || t.contains("茶") || t.contains("饼干") ||
            t.contains("方便面") || t.contains("糖") || t.contains("食品") -> "零食"

            t.contains("电池") || t.contains("纸巾") || t.contains("抽纸") || t.contains("垃圾袋") ||
            t.contains("打印纸") || t.contains("滤芯") || t.contains("牙膏") || t.contains("耗材") -> "耗材"

            t.contains("会员") || t.contains("订阅") || t.contains("icloud") || t.contains("chatgpt") ||
            t.contains("年卡") || t.contains("月卡") || t.contains("宽带") -> "网络订阅"

            t.contains("身份证") || t.contains("护照") || t.contains("房产证") || t.contains("结婚证") ||
            t.contains("户口本") || t.contains("首饰") || t.contains("金条") -> "贵重证件"

            else -> "日用品"
        }
    }

    private fun extractPrice(text: String): Double {
        // 匹配价格，如：65.50, ¥99, 128元, 实付：45.00
        val p = Pattern.compile("(?:¥|￥|\\$|金额|单价|实付|总计|小计|售价|价格)?\\s*([0-9]+\\.[0-9]{1,2}|[1-9][0-9]{0,4})\\s*(?:元|块)?")
        val m = p.matcher(text)
        val matches = mutableListOf<Double>()
        while (m.find()) {
            val numStr = m.group(1)
            val v = numStr?.toDoubleOrNull()
            if (v != null && v > 0.0 && v < 100000.0) {
                matches.add(v)
            }
        }
        return matches.lastOrNull() ?: matches.firstOrNull() ?: 0.0
    }

    private fun extractQty(text: String): Int {
        val p = Pattern.compile("([1-9][0-9]{0,3})\\s*(?:件|台|个|套|张|片|包|箱|瓶|盒|本|支|袋|罐|双|份)")
        val m = p.matcher(text)
        if (m.find()) {
            return m.group(1)?.toIntOrNull() ?: 1
        }
        return 1
    }

    private fun extractUnit(text: String): String {
        val p = Pattern.compile("[0-9]+\\s*(件|台|个|套|张|片|包|箱|瓶|盒|本|支|袋|罐|双|份)")
        val m = p.matcher(text)
        if (m.find()) {
            return m.group(1) ?: "件"
        }
        return "件"
    }

    private fun extractDate(text: String, keywords: List<String>): Long {
        val sdfList = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()),
            SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        )

        // 优先在关键字附近查找
        for (kw in keywords) {
            val idx = text.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val sub = text.substring(idx, (idx + 30).coerceAtMost(text.length))
                val date = findDateInString(sub, sdfList)
                if (date > 0) return date
            }
        }

        return findDateInString(text, sdfList)
    }

    private fun findDateInString(text: String, formats: List<SimpleDateFormat>): Long {
        val p = Pattern.compile("(20[1-3][0-9])[-/年.](0?[1-9]|1[0-2])[-/月.](0?[1-9]|[12][0-9]|3[01])(?:日)?")
        val m = p.matcher(text)
        if (m.find()) {
            val dateStr = m.group(0) ?: return 0L
            for (sdf in formats) {
                try {
                    val d = sdf.parse(dateStr)
                    if (d != null) return d.time
                } catch (_: Exception) {}
            }
        }
        return 0L
    }

    private fun cleanBrandName(raw: String): String {
        return raw.replace(Regex("[0-9]+\\.[0-9]{1,2}"), "")
            .replace(Regex("[¥￥$0-9件台个套张片包箱瓶盒本支袋罐双元块]"), "")
            .replace(Regex("[：:，,。!！*#\\[\\]()（）]"), "")
            .replace("单价", "")
            .replace("购买", "")
            .replace("保质期", "")
            .replace("生产日期", "")
            .trim()
    }
}
