package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object WorkspaceRecords {
    fun ocrResult(current: JSONObject?, requestId: String, text: String?, failure: String?): JSONObject? {
        if (current == null || current.optString("ocrRequest") != requestId) return null
        return JSONObject(current.toString()).apply {
            remove("ocrRequest")
            if (text != null) put("ocr", text)
            put("error", failure.orEmpty())
            if (optString("status") == "processing") put("status", if (failure == null) "processed" else "error")
        }
    }
    fun inbox(text: String, photo: String = "", now: Long = System.currentTimeMillis()): JSONObject {
        require(text.isNotBlank() || photo.isNotBlank()) { "内容不能为空" }
        require(text.length <= 1000000) { "文字过长" }
        return JSONObject().put("id", UUID.randomUUID().toString()).put("title", text.lineSequence().firstOrNull()?.take(80).orEmpty().ifBlank { "图片收集" })
            .put("original", text).put("photo", photo).put("status", "pending").put("error", "").put("createdAt", now)
    }

    fun link(existing: JSONArray, left: String, right: String): JSONArray {
        require(left != right && left.contains(':') && right.contains(':')) { "关联目标无效" }
        val pair = listOf(left, right).sorted()
        val result = JSONArray(existing.toString())
        for (i in 0 until result.length()) {
            val edge = result.getJSONObject(i)
            if (listOf(edge.getString("left"), edge.getString("right")).sorted() == pair) return result
        }
        return result.put(JSONObject().put("id", UUID.randomUUID().toString()).put("left", pair[0]).put("right", pair[1]))
    }

    fun related(edges: JSONArray, reference: String): List<String> = (0 until edges.length()).mapNotNull { i ->
        val edge = edges.getJSONObject(i)
        when (reference) { edge.optString("left") -> edge.optString("right"); edge.optString("right") -> edge.optString("left"); else -> null }
    }.distinct()

    fun shouldNotify(state: JSONObject?, cycle: String, now: Long): Boolean {
        if (state == null) return true
        if (state.optBoolean("muted") || state.optLong("snoozedUntil") > now) return false
        return state.optString("cycle") != cycle || (!state.optBoolean("done") && state.optLong("sentAt") == 0L)
    }

    fun reminderAction(state: JSONObject, action: String, now: Long = System.currentTimeMillis()): JSONObject = JSONObject(state.toString()).apply {
        when (action) {
            "done" -> put("done", true).put("snoozedUntil", 0L)
            "snooze" -> put("done", false).put("sentAt", 0L).put("snoozedUntil", now + 24L * 60 * 60 * 1000)
            "mute" -> put("muted", true)
            "enable" -> put("muted", false).put("done", false).put("sentAt", 0L).put("snoozedUntil", 0L)
            else -> error("未知提醒操作")
        }
    }
}
