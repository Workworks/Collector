package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * 💡 灵感想法舱与 📰 智能剪藏知识库核心逻辑单元测试
 */
class IdeaClippingVaultTest {

    @Test
    fun `IdeaRecord 模型默认值与预览截断逻辑正常`() {
        val idea = IdeaRecord(
            id = "test-idea-1",
            content = "今天读了《置身事内》，地方政府的投融资模式对地方经济发展有极为深远的影响，值得深入学习思考。",
            tags = listOf("读书", "经济", "思考"),
            moodEmoji = "💡",
            isPinned = true,
            colorHex = "#10B981"
        )

        assertEquals("test-idea-1", idea.id)
        assertTrue(idea.isPinned)
        assertEquals(3, idea.tags.size)
        assertTrue(idea.getPreview(20).endsWith("..."))
        assertEquals("今天读了《置身事内》，地方政府的投融资模...", idea.getPreview(20))
    }

    @Test
    fun `ClippingRecord 搜索关键字穿透与提取正常`() {
        val clip = ClippingRecord(
            id = "test-clip-1",
            title = "家庭局域网 P2P 增量同步架构",
            sourcePlatform = "screenshot",
            ocrRawText = "基于 UDP 广播 8849 端口与 Last-Write-Wins 时间戳仲裁算法",
            summary = "实现无中心服务器的本地数据安全对撞",
            tags = listOf("架构", "网络", "P2P")
        )

        val searchable = clip.getSearchableContent()
        assertTrue(searchable.contains("家庭局域网"))
        assertTrue(searchable.contains("8849"))
        assertTrue(searchable.contains("Last-Write-Wins"))
        assertTrue(searchable.contains("P2P"))
    }
}
