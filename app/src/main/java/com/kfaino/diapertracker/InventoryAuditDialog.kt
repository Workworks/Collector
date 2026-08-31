package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * 房间/收纳箱沉浸式实物巡检与大盘点模式 (Physical Inventory Audit Mode)
 * - 沉浸式盲盘核验流程
 * - 实时记录在位确认、数量差异、移位与缺失
 * - 盘点完成后自动生成《实物盘点差异报告》并更新核对打卡时间戳
 */
object InventoryAuditDialog {

    data class AuditItemState(
        val entry: Entry,
        var status: String = "pending", // "present", "adjusted", "missing", "pending"
        var actualQty: Int = entry.qty,
        var note: String = ""
    )

    fun startAudit(activity: Activity, store: DataStore, onCompleted: () -> Unit) {
        val houses = store.getHouses()
        val allRooms = mutableListOf("🌟 全空间所有资产")
        for (h in houses) {
            for (r in h.rooms) {
                allRooms.add("${h.name} · ${r.name}")
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("📋 选择大盘点目标区域")
            .setItems(allRooms.toTypedArray()) { _, which ->
                val selectedTarget = allRooms[which]
                val allEntries = store.loadAll().filter { it.isIn && !it.isRetired }

                val auditEntries = if (which == 0) {
                    allEntries
                } else {
                    val parts = selectedTarget.split(" · ")
                    val hName = parts[0]
                    val rName = parts.getOrNull(1) ?: ""
                    allEntries.filter { it.houseName == hName && (it.roomName == rName || it.location.contains(rName)) }
                }

                if (auditEntries.isEmpty()) {
                    Toast.makeText(activity, "所选区域暂无在库资产记录！", Toast.LENGTH_SHORT).show()
                    return@setItems
                }

                showAuditSessionDialog(activity, store, selectedTarget, auditEntries, onCompleted)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAuditSessionDialog(
        activity: Activity,
        store: DataStore,
        targetName: String,
        entries: List<Entry>,
        onCompleted: () -> Unit
    ) {
        val auditStates = entries.map { AuditItemState(it) }.toMutableList()

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
        }

        val titleTv = TextView(activity).apply {
            text = "📋 正在盘点: $targetName"
            textSize = 17f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        }

        val progressTv = TextView(activity).apply {
            text = "进度: 0 / ${auditStates.size} 项"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 4, 0, 12)
        }

        dialogView.addView(titleTv)
        dialogView.addView(progressTv)

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 800)
        }
        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        dialogView.addView(scroll)

        fun updateProgress() {
            val doneCount = auditStates.count { it.status != "pending" }
            progressTv.text = "盘点进度: $doneCount / ${auditStates.size} 项"
        }

        fun renderAuditList() {
            listContainer.removeAllViews()
            for ((idx, state) in auditStates.withIndex()) {
                val itemCard = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(when (state.status) {
                        "present" -> Color.parseColor("#064E3B")
                        "adjusted" -> Color.parseColor("#78350F")
                        "missing" -> Color.parseColor("#7F1D1D")
                        else -> Color.parseColor("#1E293B")
                    })
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = 10
                    layoutParams = lp
                }

                val rowHeader = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val nameTv = TextView(activity).apply {
                    text = "${idx + 1}. ${state.entry.brand}"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    paint.isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val qtyTv = TextView(activity).apply {
                    text = "账面: ${state.entry.qty} ${state.entry.unit}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#94A3B8"))
                }

                rowHeader.addView(nameTv)
                rowHeader.addView(qtyTv)
                itemCard.addView(rowHeader)

                val locTv = TextView(activity).apply {
                    text = "📍 放置位置: ${if (state.entry.location.isNotBlank()) state.entry.location else state.entry.roomName}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#64748B"))
                    setPadding(0, 4, 0, 8)
                }
                itemCard.addView(locTv)

                // 操作按钮组
                val btnRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val btnPresent = Button(activity).apply {
                    text = "✅ 确认在位"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, 76, 1f).apply { marginEnd = 6 }
                    setOnClickListener {
                        state.status = "present"
                        state.actualQty = state.entry.qty
                        renderAuditList()
                        updateProgress()
                    }
                }

                val btnAdjust = Button(activity).apply {
                    text = "🔢 数量有误"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, 76, 1f).apply { marginEnd = 6 }
                    setOnClickListener {
                        val numInput = EditText(activity).apply {
                            setText(state.actualQty.toString())
                            inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        }
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("修正实物数量")
                            .setMessage("输入【${state.entry.brand}】的实际在库数量：")
                            .setView(numInput)
                            .setPositiveButton("确认") { _, _ ->
                                val v = numInput.text.toString().toIntOrNull() ?: state.entry.qty
                                state.actualQty = v
                                state.status = "adjusted"
                                renderAuditList()
                                updateProgress()
                            }
                            .show()
                    }
                }

                val btnMissing = Button(activity).apply {
                    text = "❌ 缺失未找到"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, 76, 1f)
                    setOnClickListener {
                        state.status = "missing"
                        state.actualQty = 0
                        renderAuditList()
                        updateProgress()
                    }
                }

                btnRow.addView(btnPresent)
                btnRow.addView(btnAdjust)
                btnRow.addView(btnMissing)
                itemCard.addView(btnRow)

                listContainer.addView(itemCard)
            }
        }

        renderAuditList()

        MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setPositiveButton("完成盘点并生成差异报告") { _, _ ->
                finishAuditAndShowReport(activity, store, targetName, auditStates, onCompleted)
            }
            .setNegativeButton("中断退出", null)
            .show()
    }

    private fun finishAuditAndShowReport(
        activity: Activity,
        store: DataStore,
        targetName: String,
        auditStates: List<AuditItemState>,
        onCompleted: () -> Unit
    ) {
        val totalCount = auditStates.size
        val presentCount = auditStates.count { it.status == "present" }
        val adjustedList = auditStates.filter { it.status == "adjusted" }
        val missingList = auditStates.filter { it.status == "missing" }

        // 保存盘点更新 (更新数量与核对时间戳)
        val allEntries = store.loadAll().toMutableList()
        val now = System.currentTimeMillis()

        for (state in auditStates) {
            val idx = allEntries.indexOfFirst { it.id == state.entry.id }
            if (idx != -1) {
                val orig = allEntries[idx]
                if (state.status == "present") {
                    allEntries[idx] = orig.copy(lastCheckedAt = now)
                } else if (state.status == "adjusted") {
                    allEntries[idx] = orig.copy(qty = state.actualQty, lastCheckedAt = now)
                } else if (state.status == "missing") {
                    // 标记缺失
                    allEntries[idx] = orig.copy(notes = "${orig.notes} (盘点缺失)".trim(), lastCheckedAt = now)
                }
            }
        }
        store.saveAll(allEntries)

        // 生成差异报告文本
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val reportText = buildString {
            append("📊【Collecter 实物资产盘点报告】\n")
            append("--------------------------------\n")
            append("盘点区域: $targetName\n")
            append("盘点时间: ${sdf.format(Date(now))}\n")
            append("盘点总项: $totalCount 项\n")
            append("完全吻合: $presentCount 项\n")
            append("数量差异: ${adjustedList.size} 项\n")
            append("缺失未找: ${missingList.size} 项\n")
            append("--------------------------------\n")
            if (adjustedList.isNotEmpty()) {
                append("[数量调整明细]\n")
                for (a in adjustedList) {
                    append(" • ${a.entry.brand}: 原 ${a.entry.qty} → 实盘 ${a.actualQty} ${a.entry.unit}\n")
                }
                append("\n")
            }
            if (missingList.isNotEmpty()) {
                append("[缺失物品明细]\n")
                for (m in missingList) {
                    append(" • ${m.entry.brand} (原放置于: ${m.entry.location})\n")
                }
            }
            if (adjustedList.isEmpty() && missingList.isEmpty()) {
                append("🎉 恭喜！所盘点区域账物 100% 完美吻合，无任何差异！\n")
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("🎉 盘点完成 · 差异报告")
            .setMessage(reportText)
            .setPositiveButton("复制报告") { _, _ ->
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Collecter 盘点报告", reportText))
                Toast.makeText(activity, "已复制盘点报告到剪贴板！", Toast.LENGTH_SHORT).show()
                onCompleted()
            }
            .setNegativeButton("关闭") { _, _ ->
                onCompleted()
            }
            .show()
    }
}
