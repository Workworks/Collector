package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🔐 更新下载源优先级测试。
 *
 * 覆盖 GEMINI.md 铁律 3 的安全不变量：更新只允许 GitHub 官方源。
 *
 * 这条曾经被破坏过一次 —— commit `ed0ce33` 为了「修复下载超时」把三个第三方代理
 * 提到了官方源前面。本测试就是防止同样的事再发生第二次。
 */
class UpdateSourceTest {

    private val officialApk =
        "https://github.com/Workworks/Collector/releases/download/v4.2.0/Collecter.apk"

    @Test
    fun `官方源必须排在候选列表第一位`() {
        val candidates = UpdateSource.candidates(officialApk)

        assertEquals(
            "第一个候选必须是官方直连地址，不允许任何代理排在它前面",
            officialApk,
            candidates.first()
        )
    }

    @Test
    fun `官方资源不得经第三方代理转发`() {
        val candidates = UpdateSource.candidates(officialApk)
        assertEquals(listOf(officialApk), candidates)
    }

    @Test
    fun `已失效证书代理不得保留在候选列表`() {
        val candidates = UpdateSource.candidates(officialApk)
        assertFalse(candidates.any { it.contains("ghfast.top") || it.contains("ghproxy") })
        assertEquals(listOf("GitHub 官方"), candidates.map(UpdateSource::label))
    }

    @Test
    fun `Releases API 地址走官方域名且官方优先`() {
        val api = UpdateSource.latestReleaseApi("Workworks/Collector")

        assertEquals(
            "https://api.github.com/repos/Workworks/Collector/releases/latest",
            api
        )
        assertEquals(api, UpdateSource.candidates(api).first())
    }

    @Test
    fun `非官方地址不得被转发给第三方代理`() {
        val thirdParty = "https://example.com/some/file.apk"

        val candidates = UpdateSource.candidates(thirdParty)

        assertEquals(
            "非 GitHub 官方地址应原样返回，避免把任意 URL 交给第三方代理",
            listOf(thirdParty),
            candidates
        )
    }

    @Test
    fun `空地址返回空列表`() {
        assertTrue(UpdateSource.candidates("").isEmpty())
        assertTrue(UpdateSource.candidates("   ").isEmpty())
    }

    @Test
    fun `官方域名识别正确`() {
        assertTrue(UpdateSource.isOfficial("https://api.github.com/repos/a/b/releases/latest"))
        assertTrue(UpdateSource.isOfficial(officialApk))
        assertFalse(UpdateSource.isOfficial("https://ghfast.top/$officialApk"))
        assertFalse(UpdateSource.isOfficial("https://evil.example.com/github.com/x.apk"))
    }
}
