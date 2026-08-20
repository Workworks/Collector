package com.kfaino.diapertracker

/**
 * 🔐 更新下载源优先级统一策略 (Update Source Policy)
 *
 * ## 安全不变量（见 GEMINI.md 铁律 3，禁止为"提速/修超时"而调整顺序）
 * 官方源（api.github.com / github.com）**永远排在候选列表第一位**；
 * 第三方 CDN 代理只能作为官方源不可达时的容灾 fallback。
 *
 * 原因：第三方代理可以任意替换响应体。本应用会下载 APK 与包含 dex 的热补丁包，
 * 一旦代理被投毒，等于让攻击者在存有全家证件、保单与加密密码的进程里执行任意代码。
 * 慢是体验问题，投毒是安全问题，两者不可交换。
 */
object UpdateSource {

    /** 第三方加速代理，仅作为容灾 fallback 使用 */
    private val PROXY_PREFIXES = listOf(
        "https://ghfast.top/",
        "https://mirror.ghproxy.com/",
        "https://ghproxy.net/"
    )

    private val OFFICIAL_HOST_PREFIXES = listOf(
        "https://api.github.com/",
        "https://github.com/",
        "https://objects.githubusercontent.com/",
        "https://release-assets.githubusercontent.com/"
    )

    /**
     * 构建下载候选列表：官方直连优先，随后依次尝试第三方代理。
     *
     * @param officialUrl 官方原始地址（api.github.com 或 github.com 的 release 资源地址）
     */
    fun candidates(officialUrl: String): List<String> {
        if (officialUrl.isBlank()) return emptyList()
        if (!isOfficial(officialUrl)) {
            // 非官方地址不套代理，原样返回，避免把任意 URL 转发给第三方
            return listOf(officialUrl)
        }
        return listOf(officialUrl) + PROXY_PREFIXES.map { it + officialUrl }
    }

    /** 判断该地址是否为 GitHub 官方域名 */
    fun isOfficial(url: String): Boolean =
        OFFICIAL_HOST_PREFIXES.any { url.startsWith(it) }

    /** GitHub Releases API：查询最新 Release */
    fun latestReleaseApi(repo: String): String =
        "https://api.github.com/repos/$repo/releases/latest"
}
