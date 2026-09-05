package com.kfaino.diapertracker

import com.kfaino.collecter.core.WorkspaceRecords
import org.json.JSONObject

object FindBackSummary {
    data class Attachment(val label: String, val filename: String)
    data class Related(val reference: String, val title: String)
    data class Summary(
        val reference: String,
        val title: String,
        val category: String,
        val location: String,
        val notes: String,
        val attachments: List<Attachment>,
        val related: List<Related>
    )

    fun build(document: JSONObject, reference: String): Summary? {
        val record = find(document, reference) ?: return null
        val attachments = buildList {
            record.first("img_p", "photoPath", "photo").takeIf(String::isNotBlank)?.let { add(Attachment("实物照片", it)) }
            record.first("rec_p", "receiptPath").takeIf(String::isNotBlank)?.let { add(Attachment("发票 / 保修卡", it)) }
        }
        val edges = document.optJSONArray("links") ?: org.json.JSONArray()
        val related = WorkspaceRecords.related(edges, reference).mapNotNull { target ->
            find(document, target)?.let { Related(target, title(it)) }
        }
        return Summary(
            reference,
            title(record),
            record.first("cat", "category").ifBlank { "未分类" },
            record.first("loc", "location").ifBlank { "未整理" },
            record.first("notes", "note"),
            attachments,
            related
        )
    }

    private fun find(document: JSONObject, reference: String): JSONObject? {
        val collection = reference.substringBefore(':')
        val id = reference.substringAfter(':', "")
        if (id.isBlank()) return null
        val records = document.optJSONArray(collection) ?: return null
        return (0 until records.length()).map { records.optJSONObject(it) }.firstOrNull { it?.optString("id") == id }
    }

    private fun title(record: JSONObject): String = sequenceOf("title", "brand", "name", "content", "original")
        .map(record::optString).firstOrNull(String::isNotBlank)?.take(80) ?: "未命名记录"

    private fun JSONObject.first(vararg keys: String): String = keys.asSequence().map(::optString).firstOrNull(String::isNotBlank).orEmpty()
}
