package com.kfaino.diapertracker

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object SearchResultDetailDialog {
    fun show(activity: Activity, reference: String) {
        val workspace = CollectionWorkspace(activity)
        val summary = FindBackSummary.build(workspace.document(), reference) ?: run {
            android.widget.Toast.makeText(activity, "记录已不存在", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val actions = summary.attachments.map { "打开${it.label}" } + summary.related.map { "关联 · ${it.title}" }
        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle("🔎 ${summary.title}")
            .setMessage("📍 ${summary.location}\n${summary.category}${summary.notes.takeIf(String::isNotBlank)?.let { "\n备注：$it" }.orEmpty()}" +
                if (actions.isEmpty()) "\n\n尚未关联照片或资料" else "")
            .setNegativeButton("关闭", null)
        if (actions.isNotEmpty()) builder.setItems(actions.toTypedArray()) { _, index ->
            if (index < summary.attachments.size) {
                val attachment = summary.attachments[index]
                PhotoPreviewDialog.show(activity, "${summary.title} · ${attachment.label}", attachment.filename)
            } else {
                show(activity, summary.related[index - summary.attachments.size].reference)
            }
        }
        builder.show()
    }
}
