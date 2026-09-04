package com.kfaino.diapertracker

import org.json.JSONArray

object RecallPolicy {
    data class Prompt(val key: String, val title: String, val content: String, val cycle: String)
    fun evaluate(entries: List<Entry>, inbox: JSONArray, now: Long): List<Prompt> {
        val result = mutableListOf<Prompt>()
        val pending = (0 until inbox.length()).map { inbox.getJSONObject(it) }.filter { it.optString("status", "pending") != "organized" }
        val oldest = pending.minOfOrNull { it.optLong("createdAt", now) } ?: now
        if (pending.size >= 5 && now - oldest >= 3L * 24 * 60 * 60 * 1000) {
            result += Prompt("inbox-backlog", "收集箱有 ${pending.size} 条待整理", "花一分钟补一个名称或位置即可，其余内容可以继续保留。", (now / (7L * 24 * 60 * 60 * 1000)).toString())
        }
        val unlocated = entries.filter { !it.isRetired && (it.location.isBlank() || it.location == "未整理") && now - it.ts >= 30L * 24 * 60 * 60 * 1000 }
        if (unlocated.isNotEmpty()) {
            result += Prompt("unlocated-items", "${unlocated.size} 件物品还没记录位置", "最近需要时可能不好找，补充常用位置即可。", (now / (30L * 24 * 60 * 60 * 1000)).toString())
        }
        return result
    }
}
