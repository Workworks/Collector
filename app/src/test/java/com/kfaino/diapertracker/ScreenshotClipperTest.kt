package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test

/**
 * 📸 截图无感监听与 🔗 网页正文深度剪藏引擎核心逻辑单元测试
 */
class ScreenshotClipperTest {

    @Test
    fun `截图文本智能分析提取标题与电商标签`() {
        val ocrText = """
            京东商城 订单详情
            安格斯原切牛排 1kg 顺丰冷链
            实付款：¥198.00
            配送地址：上海市浦东新区...
        """.trimIndent()

        val result = ScreenshotOcrProcessor.analyzeScreenshotText(ocrText)
        assertEquals("京东商城 订单详情", result.title)
        assertTrue(result.tags.contains("电商订单"))
        assertTrue(result.tags.contains("截图快照"))
        assertTrue(result.summary.contains("实付款：¥198.00"))
    }

    @Test
    fun `截图文本智能分析提取技术与食谱标签`() {
        val codeText = """
            Kotlin 协程最佳实践
            fun main() = runBlocking {
                val flow = flowOf(1, 2, 3)
            }
        """.trimIndent()
        val techResult = ScreenshotOcrProcessor.analyzeScreenshotText(codeText)
        assertTrue(techResult.tags.contains("技术资料"))

        val recipeText = """
            香煎西冷牛排烹饪做法
            配料：海盐、黑胡椒碎、黄油、迷迭香
            调味步骤：热锅下油，大火煎至两面焦黄...
        """.trimIndent()
        val recipeResult = ScreenshotOcrProcessor.analyzeScreenshotText(recipeText)
        assertTrue(recipeResult.tags.contains("美食食谱"))
    }

    @Test
    fun `网页剪藏平台识别与标题提取准确`() {
        assertEquals("wechat", WebClipperEngine.detectPlatform("https://mp.weixin.qq.com/s/abcdef123"))
        assertEquals("zhihu", WebClipperEngine.detectPlatform("https://zhuanlan.zhihu.com/p/123456"))
        assertEquals("juejin", WebClipperEngine.detectPlatform("https://juejin.cn/post/987654"))
        assertEquals("xiaohongshu", WebClipperEngine.detectPlatform("https://www.xiaohongshu.com/explore/112233"))
        assertEquals("web", WebClipperEngine.detectPlatform("https://github.com/Workworks/Collector"))

        val sampleHtml = "<html><head><title>深入理解分布式系统架构 - 掘金</title></head><body><h1>正文标题</h1></body></html>"
        val title = WebClipperEngine.extractTitle(sampleHtml)
        assertEquals("深入理解分布式系统架构 - 掘金", title)
    }

    @Test
    fun `HTML清洗转换为纯净Markdown正文`() {
        val rawHtml = """
            <nav>导航菜单不应包含</nav>
            <script>var x = 1;</script>
            <style>.hide { display:none; }</style>
            <h1>全态资产收纳</h1>
            <p>基于 100% 离线私有沙盒存储&amp;无中心化云端依赖。</p>
            <p>第一性原理&nbsp;&ldquo;12大专业馆&rdquo;全量覆盖。</p>
            <footer>页脚版权信息不应包含</footer>
        """.trimIndent()

        val markdown = WebClipperEngine.cleanHtmlToMarkdown(rawHtml)
        assertFalse(markdown.contains("导航菜单"))
        assertFalse(markdown.contains("var x = 1"))
        assertFalse(markdown.contains("页脚版权"))
        assertTrue(markdown.contains("# 全态资产收纳"))
        assertTrue(markdown.contains("100% 离线私有沙盒存储&无中心化云端依赖。"))
        assertTrue(markdown.contains("第一性原理 “12大专业馆”全量覆盖。"))
    }

    @Test
    fun `截图文件命名与路径判断识别准确`() {
        assertTrue(ScreenshotWatcherHelper.isScreenshot("Screenshot_20260829_091500.png", "/storage/emulated/0/DCIM/Screenshots/Screenshot_1.png"))
        assertTrue(ScreenshotWatcherHelper.isScreenshot("截屏_2026-08-29.jpg", "/storage/emulated/0/Pictures/Screenshots/1.jpg"))
        assertTrue(ScreenshotWatcherHelper.isScreenshot("screen_123.jpg", "/storage/emulated/0/Pictures/screen_123.jpg"))
        assertFalse(ScreenshotWatcherHelper.isScreenshot("IMG_20260829_091500.jpg", "/storage/emulated/0/DCIM/Camera/IMG_1.jpg"))
    }
}
