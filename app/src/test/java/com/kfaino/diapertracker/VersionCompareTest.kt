package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🔢 版本比较逻辑测试。
 * 覆盖 UpdateManager.isNewerVersion 的边界与容错测试。
 */
class VersionCompareTest {

    @Test
    fun `浏览器下载只接受 GitHub 官方地址并优先 APK`() {
        val apk = "https://github.com/Workworks/Collector/releases/download/v4.3.8/app-release.apk"
        val release = "https://github.com/Workworks/Collector/releases/tag/v4.3.8"
        assertEquals(apk, UpdateManager.resolveOfficialDownloadUrl(apk, release))
        assertEquals(release, UpdateManager.resolveOfficialDownloadUrl("https://example.com/app.apk", release))
        assertNull(UpdateManager.resolveOfficialDownloadUrl("https://example.com/app.apk", "https://evil.example/release"))
    }

    @Test
    fun `常规三段版本号升级判定`() {
        assertTrue("4.2.1 应比 4.2.0 新", UpdateManager.isNewerVersion("4.2.1", "4.2.0"))
        assertTrue("4.3.0 应比 4.2.9 新", UpdateManager.isNewerVersion("4.3.0", "4.2.9"))
        assertTrue("5.0.0 应比 4.9.9 新", UpdateManager.isNewerVersion("5.0.0", "4.9.9"))
    }

    @Test
    fun `带有大小写 v 前缀版本号正常兼容`() {
        assertTrue("v4.2.1 应比 4.2.0 新", UpdateManager.isNewerVersion("v4.2.1", "4.2.0"))
        assertTrue("V4.2.1 应比 v4.2.0 新", UpdateManager.isNewerVersion("V4.2.1", "v4.2.0"))
        assertFalse("v4.2.0 不应比 4.2.0 新", UpdateManager.isNewerVersion("v4.2.0", "4.2.0"))
    }

    @Test
    fun `相同版本与降级版本判定为假`() {
        assertFalse("相同版本不应提示更新", UpdateManager.isNewerVersion("4.2.0", "4.2.0"))
        assertFalse("低版本不应提示更新", UpdateManager.isNewerVersion("4.1.9", "4.2.0"))
        assertFalse("历史大版本不应提示更新", UpdateManager.isNewerVersion("3.9.0", "4.0.0"))
    }

    @Test
    fun `不同分段长度版本号判定`() {
        assertTrue("4.2.0.1 应比 4.2.0 新", UpdateManager.isNewerVersion("4.2.0.1", "4.2.0"))
        assertFalse("4.2 不应比 4.2.0 新", UpdateManager.isNewerVersion("4.2", "4.2.0"))
        assertTrue("4.2.1 应比 4.2 新", UpdateManager.isNewerVersion("4.2.1", "4.2"))
    }
}
