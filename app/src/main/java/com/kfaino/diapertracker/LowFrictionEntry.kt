package com.kfaino.diapertracker

import android.app.Activity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton

object LowFrictionEntry {
    fun showQuickAdd(activity: Activity, store: DataStore, onSaved: () -> Unit = {}) {
        val input = EditText(activity).apply {
            hint = "例如：客厅遥控器"
            maxLines = 1
            setSingleLine(true)
            setPadding(48, 20, 48, 20)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("记一件物品")
            .setMessage("只填名称就能保存，分类、数量和时间已自动设置。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setNeutralButton("完整填写") { _, _ -> (activity as? MainActivity)?.showAddDialog() }
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val current = store.loadAll()
                val entry = QuickEntryFactory.create(input.text.toString(), history = current)
                if (entry == null) {
                    input.error = "请输入物品名称"
                    return@setOnClickListener
                }
                val entries = current.toMutableList()
                if (entries.isEmpty()) store.snoozeBackupPrompt(3)
                entries.add(entry)
                store.saveAll(entries)
                Toast.makeText(activity, "已记下「${entry.brand}」", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSaved()
                if (store.consumeContextTip("reminder")) showSimpleReminder(activity, store, entry, onSaved)
            }
            input.requestFocus()
        }
        dialog.show()
    }

    private fun showSimpleReminder(activity: Activity, store: DataStore, entry: Entry, onSaved: () -> Unit) {
        val options = arrayOf("不开启", "到期提醒", "定期检查")
        MaterialAlertDialogBuilder(activity).setTitle("要提醒你吗？")
            .setMessage("这是唯一一次快速询问，以后可在完整编辑中调整。")
            .setItems(options) { _, index ->
                if (index == 0) return@setItems
                val list = store.loadAll().toMutableList()
                val pos = list.indexOfFirst { it.id == entry.id }
                if (pos < 0) return@setItems
                val updated = if (index == 1) {
                    val days = QuickEntryFactory.suggest(entry.brand, list).expiryDays ?: 30
                    list[pos].copy(expiryDate = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000)
                } else list[pos].copy(isImportant = true, reminderEnabled = true, reminderIntervalDays = 30, lastCheckedAt = System.currentTimeMillis())
                list[pos] = updated
                store.saveAll(list)
                (activity as? MainActivity)?.requestReminderPermissionIfNeeded()
                Toast.makeText(activity, if (index == 1) "已设置到期提醒，可在完整编辑中改日期" else "已设置每 30 天检查", Toast.LENGTH_SHORT).show()
                onSaved()
            }.show()
    }

    fun showFirstRunIfNeeded(activity: MainActivity, store: DataStore) {
        if (store.hasSeenQuickStart()) return
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            setBackgroundResource(R.drawable.bg_dialog_card)
        }
        content.addView(TextView(activity).apply {
            text = "你现在想记什么？"
            textSize = 22f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            paint.isFakeBoldText = true
        })
        content.addView(TextView(activity).apply {
            text = "不用先建分类或账本，选一种方式就能开始。"
            textSize = 14f
            setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            setPadding(0, dp(8), 0, dp(14))
        })
        val dialog = MaterialAlertDialogBuilder(activity).setView(content).setCancelable(true).create()
        val actions = listOf(
            "📷 拍照收纳" to 0,
            "＋ 记下物品" to 1,
            "🔗 粘贴链接" to 2,
            "以后再整理" to 3
        )
        actions.forEachIndexed { position, (label, index) ->
            content.addView(MaterialButton(activity).apply {
                text = label
                textSize = 15f
                isAllCaps = false
                cornerRadius = dp(14)
                minimumHeight = dp(50)
                if (position == 0) {
                    backgroundTintList = ContextCompat.getColorStateList(activity, R.color.primary)
                    setTextColor(ContextCompat.getColor(activity, R.color.white))
                } else {
                    backgroundTintList = ContextCompat.getColorStateList(activity, R.color.input_bg)
                    setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                    strokeColor = ContextCompat.getColorStateList(activity, R.color.card_border)
                    strokeWidth = dp(1)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply { bottomMargin = if (position == actions.lastIndex) 0 else dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                store.markQuickStartSeen()
                when (index) {
                    0 -> activity.collectPhotoToInbox()
                    1 -> showQuickAdd(activity, store) { activity.refreshCurrentFragment() }
                    2 -> activity.collectLinkToInbox()
                }
            }
            })
        }
        dialog.setOnCancelListener { store.markQuickStartSeen() }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
