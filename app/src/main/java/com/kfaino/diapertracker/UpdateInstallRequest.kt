package com.kfaino.diapertracker

import org.json.JSONObject
import java.io.File

internal object UpdateInstallRequest {
    const val MAX_AGE_MS = 10 * 60 * 1000L

    data class Pending(
        val apkPath: String,
        val expectedSize: Long,
        val expectedSha256: String,
        val requestedAt: Long
    )

    fun encode(pending: Pending): String = JSONObject()
        .put("apkPath", pending.apkPath)
        .put("expectedSize", pending.expectedSize)
        .put("expectedSha256", pending.expectedSha256)
        .put("requestedAt", pending.requestedAt)
        .toString()

    fun decode(raw: String?): Pending? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            Pending(
                apkPath = json.getString("apkPath"),
                expectedSize = json.getLong("expectedSize"),
                expectedSha256 = json.optString("expectedSha256"),
                requestedAt = json.getLong("requestedAt")
            )
        }.getOrNull()
    }

    fun isFresh(pending: Pending, now: Long): Boolean {
        val age = now - pending.requestedAt
        return pending.requestedAt > 0L && age in 0..MAX_AGE_MS
    }

    fun isAllowedApk(file: File, allowedRoots: List<File>): Boolean {
        if (!file.name.endsWith(".apk", ignoreCase = true)) return false
        val filePath = file.canonicalFile.toPath()
        return allowedRoots.any { root -> filePath.startsWith(root.canonicalFile.toPath()) }
    }
}
