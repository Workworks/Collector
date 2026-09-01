package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.collecter.core.WorkspaceRecords
import com.kfaino.diapertracker.databinding.DialogCollectionWorkspaceBinding
import org.json.JSONObject

object CollectionWorkspaceDialog {
    private fun statusLabel(status: String) = when (status) {
        "pending" -> "待整理"
        "processing" -> "处理中"
        "processed" -> "已提取文字"
        "organized" -> "已整理"
        "error" -> "处理失败，可重试"
        else -> status
    }
    fun show(activity: Activity) {
        val workspace = CollectionWorkspace(activity)
        val records = workspace.records("inbox")
        val list = (0 until records.length()).map { records.getJSONObject(it) }.reversed()
        val binding = DialogCollectionWorkspaceBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.workspaceTitle.text = "收集箱 · ${list.size} 条"
        binding.workspaceEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.workspaceRecordScroll.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

        val density = activity.resources.displayMetrics.density
        list.forEach { record ->
            binding.workspaceRecordList.addView(TextView(activity).apply {
                text = "${workspace.title(record)}\n${statusLabel(record.optString("status", "pending"))}"
                textSize = 15f
                setTextColor(activity.getColor(R.color.text_primary))
                setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
                background = activity.getDrawable(R.drawable.bg_input_box)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (8 * density).toInt()
                }
                setOnClickListener {
                    dialog.dismiss()
                    showRecord(activity, "inbox:${record.getString("id")}")
                }
            })
        }
        binding.workspaceClose.setOnClickListener { dialog.dismiss() }
        binding.workspaceAddText.setOnClickListener {
            dialog.dismiss()
            val input = EditText(activity).apply { hint = "先保存原文，稍后整理"; minLines = 3 }
            MaterialAlertDialogBuilder(activity).setTitle("快速收集").setView(input).setNegativeButton("取消", null)
                .setPositiveButton("保存") { _, _ -> safely(activity) { workspace.addText(input.text.toString()); show(activity) } }.show()
        }
        binding.workspaceOpenWorkbench.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(android.content.Intent(activity, WorkbenchActivity::class.java))
        }
        dialog.show()
    }

    fun search(activity: Activity) {
        val input = EditText(activity).apply { hint = "物品、位置、发票、说明书、原文" }
        MaterialAlertDialogBuilder(activity).setTitle("找回与关联").setView(input).setNegativeButton("取消", null)
            .setPositiveButton("搜索") { _, _ ->
                val q = input.text.toString().trim()
                val workspace = CollectionWorkspace(activity)
                val root = workspace.document()
                val matches = mutableListOf<Pair<String, String>>()
                for (collection in root.keys()) {
                    if (collection in listOf("links", "reminders")) continue
                    val arr = root.getJSONArray(collection)
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        if (q.isNotEmpty() && item.toString().contains(q, ignoreCase = true)) matches.add("$collection:${item.optString("id")}" to workspace.title(item))
                    }
                }
                MaterialAlertDialogBuilder(activity).setTitle("找到 ${matches.size} 条")
                    .setItems(matches.map { it.second }.toTypedArray()) { _, i -> showRecord(activity, matches[i].first) }
                    .setNegativeButton("关闭", null).show()
            }.show()
    }

    fun showRecord(activity: Activity, reference: String) {
        val workspace = CollectionWorkspace(activity)
        val record = workspace.find(reference)
        if (record == null) { Toast.makeText(activity, "原记录已删除；关联不自动删除其他资料", Toast.LENGTH_LONG).show(); return }
        val related = workspace.related(reference)
        val details = listOf("original", "content", "markdown", "ocr", "notes", "loc", "location", "r_name", "roomName", "error")
            .mapNotNull { key -> record.optString(key).takeIf { it.isNotBlank() }?.let { "$key：$it" } }.joinToString("\n\n")
        MaterialAlertDialogBuilder(activity).setTitle(workspace.title(record)).setMessage(details.ifBlank { record.toString(2) })
            .setPositiveButton("关联资料（${related.size}）") { _, _ ->
                val labels = related.map { workspace.find(it)?.let(workspace::title) ?: "已删除：$it" } + "＋建立新关联"
                MaterialAlertDialogBuilder(activity).setTitle("双向关联").setItems(labels.toTypedArray()) { _, i ->
                    if (i == related.size) chooseLink(activity, reference)
                    else MaterialAlertDialogBuilder(activity).setTitle(labels[i]).setItems(arrayOf("打开原记录", "解除关联（保留原记录）")) { _, action ->
                        if (action == 0) showRecord(activity, related[i])
                        else safely(activity) { workspace.unlink(reference, related[i]); showRecord(activity, reference) }
                    }.show()
                }.setNegativeButton("返回", null).show()
            }.setNeutralButton("操作") { _, _ -> actions(activity, reference, record) }
            .setNegativeButton("关闭", null).show()
    }

    private fun chooseLink(activity: Activity, from: String) {
        val workspace = CollectionWorkspace(activity)
        val root = workspace.document()
        val choices = mutableListOf<Pair<String, String>>()
        for (collection in root.keys()) {
            if (collection in listOf("links", "reminders", "houses")) continue
            val arr = root.getJSONArray(collection)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val reference = "$collection:${item.optString("id")}"
                if (reference != from && item.optString("id").isNotBlank()) choices.add(reference to "$collection · ${workspace.title(item)}")
            }
        }
        MaterialAlertDialogBuilder(activity).setTitle("选择物品或资料").setItems(choices.map { it.second }.toTypedArray()) { _, i ->
            safely(activity) { workspace.link(from, choices[i].first); showRecord(activity, from) }
        }.setNegativeButton("取消", null).show()
    }

    private fun actions(activity: Activity, reference: String, record: JSONObject) {
        val workspace = CollectionWorkspace(activity)
        val isInbox = reference.startsWith("inbox:")
        val photo = sequenceOf("photo", "img_p", "rec_p").map { record.optString(it) }.firstOrNull { it.isNotBlank() }
        val choices = mutableListOf<String>()
        if (photo != null) choices.add("查看原图")
        if (isInbox && photo != null) choices.add("提取文字 / 重试 OCR")
        if (isInbox) choices.addAll(listOf("修改标题与整理状态", "删除本条收集记录"))
        MaterialAlertDialogBuilder(activity).setTitle("原件与整理").setItems(choices.toTypedArray()) { _, index ->
            when (choices[index]) {
                "查看原图" -> PhotoPreviewDialog.show(activity, workspace.title(record), photo!!)
                "提取文字 / 重试 OCR" -> {
                    safely(activity) {
                        val requestId = java.util.UUID.randomUUID().toString()
                        record.put("status", "processing").put("error", "").put("ocrRequest", requestId)
                        workspace.upsert("inbox", record)
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(photo, options)
                        options.inSampleSize = maxOf(1, maxOf(options.outWidth, options.outHeight) / 2048)
                        options.inJustDecodeBounds = false
                        val bitmap = android.graphics.BitmapFactory.decodeFile(photo, options) ?: run {
                            workspace.finishOcr(record.getString("id"), requestId, null, "图片无法读取，原件仍保留")
                            error("图片无法读取，原件仍保留")
                        }
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build())
                        recognizer.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0))
                            .addOnSuccessListener { text ->
                                workspace.finishOcr(record.getString("id"), requestId, text.text, null)
                                if (!activity.isFinishing) showRecord(activity, reference)
                            }.addOnFailureListener { error ->
                                workspace.finishOcr(record.getString("id"), requestId, null, error.message ?: "提取失败")
                                Toast.makeText(activity, "提取失败，原件已保留，可重试", Toast.LENGTH_LONG).show()
                            }.addOnCompleteListener { bitmap.recycle(); recognizer.close() }
                    }
                }
                "修改标题与整理状态" -> {
                    val input = EditText(activity).apply { setText(workspace.title(record)) }
                    MaterialAlertDialogBuilder(activity).setTitle("整理收集记录").setView(input).setNegativeButton("取消", null)
                        .setPositiveButton("标记已整理") { _, _ -> safely(activity) {
                            record.put("title", input.text.toString()).put("status", "organized")
                            workspace.upsert("inbox", record); show(activity)
                        } }.show()
                }
                "删除本条收集记录" -> MaterialAlertDialogBuilder(activity).setTitle("确认删除本条？")
                    .setMessage("关联的物品和其他资料不会删除。").setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ -> safely(activity) { workspace.remove("inbox", record.getString("id")); show(activity) } }.show()
            }
        }.setNegativeButton("取消", null).show()
    }

    fun reminders(activity: Activity) {
        val workspace = CollectionWorkspace(activity)
        val arr = workspace.records("reminders")
        val list = (0 until arr.length()).map { arr.getJSONObject(it) }
        MaterialAlertDialogBuilder(activity).setTitle("提醒处理 · ${list.size} 项")
            .setItems(list.map { "${it.optString("title", it.optString("id"))} · ${if (it.optBoolean("muted")) "已关闭" else if (it.optBoolean("done")) "已处理" else "待处理"}" }.toTypedArray()) { _, i ->
                val item = list[i]
                MaterialAlertDialogBuilder(activity).setTitle(item.optString("title")).setItems(arrayOf("完成本周期", "延后一天", "关闭此事项", "重新启用")) { _, action ->
                    safely(activity) {
                        if(action==0 && item.optString("id").startsWith("workbench:")) {
                            WorkbenchRepository(activity).execute(JSONObject().put("op","life").put("refs",org.json.JSONArray(listOf(item.getString("id").removePrefix("workbench:"))))
                                .put("action","maintenance").put("note","从到期提醒记录完成").put("nextAt",0))
                        }
                        workspace.upsert("reminders", WorkspaceRecords.reminderAction(item, listOf("done", "snooze", "mute", "enable")[action])); reminders(activity)
                    }
                }.show()
            }.setNegativeButton("关闭", null).show()
    }

    private fun safely(activity: Activity, action: () -> Unit) {
        try { action() } catch (e: Exception) {
            android.util.Log.e("CollectionWorkspace", "操作失败", e)
            Toast.makeText(activity, "操作失败：${e.message}，原件未删除", Toast.LENGTH_LONG).show()
        }
    }
}
