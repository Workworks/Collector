package com.kfaino.diapertracker

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * 🔗 网页与社交文章深度剪藏引擎 (Web Clipper Engine)
 * - 纯净抓取知乎、微信公众号、小红书与通用网页正文
 * - 剔除广告杂质与样式标签，智能转换为沉浸式离线 Markdown
 */
object WebClipperEngine {

    private const val TAG = "WebClipperEngine"
    private val executor = Executors.newSingleThreadExecutor()

    fun clipUrl(
        urlString: String,
        onComplete: (ClippingRecord?) -> Unit
    ) {
        val trimmedUrl = urlString.trim()
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            onComplete(null)
            return
        }

        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                val url = URI.create(trimmedUrl).toURL()
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "请求网页失败，状态码: $code")
                    onComplete(null)
                    return@execute
                }

                val html = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val title = extractTitle(html).ifBlank { trimmedUrl }
                val markdown = cleanHtmlToMarkdown(html)
                val platform = detectPlatform(trimmedUrl)
                val summary = if (markdown.length > 90) markdown.replace("\n", " ").take(90) + "..." else markdown.replace("\n", " ")

                val tags = mutableListOf<String>()
                tags.add(when (platform) {
                    "wechat" -> "微信长文"
                    "zhihu" -> "知乎精选"
                    "juejin" -> "掘金技术"
                    "xiaohongshu" -> "小红书"
                    else -> "网络剪藏"
                })

                val record = ClippingRecord(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    originalUrl = trimmedUrl,
                    sourcePlatform = platform,
                    fullMarkdown = markdown,
                    ocrRawText = "",
                    summary = summary,
                    tags = tags,
                    capturedAt = System.currentTimeMillis()
                )

                onComplete(record)
            } catch (e: Exception) {
                Log.w(TAG, "网页剪藏抓取解析异常: ${e.message}", e)
                onComplete(null)
            } finally {
                try {
                    conn?.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "关闭网络连接异常", e)
                }
            }
        }
    }

    fun detectPlatform(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("mp.weixin.qq.com") || lower.contains("weixin") -> "wechat"
            lower.contains("zhihu.com") -> "zhihu"
            lower.contains("juejin.cn") -> "juejin"
            lower.contains("xiaohongshu.com") || lower.contains("xhslink.com") -> "xiaohongshu"
            else -> "web"
        }
    }

    fun extractTitle(html: String): String {
        val pattern = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val raw = matcher.group(1) ?: ""
            return cleanHtmlEntities(raw).trim()
        }
        return ""
    }

    fun cleanHtmlToMarkdown(html: String): String {
        var text = html

        // 1. 移除无关的 script, style, nav, footer, header, svg, noscript 块
        val unwantedTags = listOf("script", "style", "nav", "footer", "header", "svg", "noscript", "iframe")
        for (tag in unwantedTags) {
            val p = Pattern.compile("<$tag[^>]*>.*?</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            text = p.matcher(text).replaceAll("")
        }

        // 2. 转换标题与段落
        text = text.replace(Regex("(?i)<h1[^>]*>(.*?)</h1>"), "\n\n# $1\n\n")
        text = text.replace(Regex("(?i)<h2[^>]*>(.*?)</h2>"), "\n\n## $1\n\n")
        text = text.replace(Regex("(?i)<h3[^>]*>(.*?)</h3>"), "\n\n### $1\n\n")
        text = text.replace(Regex("(?i)<h4[^>]*>(.*?)</h4>"), "\n\n#### $1\n\n")
        text = text.replace(Regex("(?i)<p[^>]*>(.*?)</p>"), "\n\n$1\n\n")
        text = text.replace(Regex("(?i)<li[^>]*>(.*?)</li>"), "\n- $1")
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?i)<hr\\s*/?>"), "\n---\n")

        // 3. 剥除所有剩余 HTML 标签
        text = text.replace(Regex("<[^>]+>"), "")

        // 4. 反转义 HTML 实体字符
        text = cleanHtmlEntities(text)

        // 5. 格式化多余空行
        text = text.replace(Regex("\n{3,}"), "\n\n").trim()

        return text
    }

    private fun cleanHtmlEntities(str: String): String {
        return str
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
    }
}
