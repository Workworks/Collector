package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogLocationHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocationHistoryDialog {

    fun show(activity: Activity, entry: Entry) {
        val binding = DialogLocationHistoryBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.historyItemTitle.text = "【${entry.brand}】位置变迁时光轴"
        binding.historyCurrentLocation.text = "📍 当前位置: " + if (entry.location.isNotBlank()) "${entry.houseName} · ${entry.location}" else "未设置具体位置"

        val container = binding.historyTimelineContainer
        container.removeAllViews()

        val records = mutableListOf<LocationMovement>()
        // 当前位置作为最新一条
        if (entry.location.isNotBlank()) {
            records.add(
                LocationMovement(
                    location = entry.location,
                    houseName = entry.houseName,
                    roomName = entry.roomName,
                    movedAt = entry.ts,
                    note = "当前处于该位置"
                )
            )
        }
        records.addAll(entry.locationHistory)

        if (records.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = "暂无位置变迁记录\n可在修改物品时更新其放置位置"
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 30, 0, 30)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(emptyTv)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for ((idx, r) in records.withIndex()) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 10, 0, 14)
                }

                val titleTv = TextView(activity).apply {
                    text = if (idx == 0) "📍 【当前】${r.houseName} · ${r.location}" else "🕒 【历史】${r.houseName} · ${r.location}"
                    textSize = 13f
                    setTextColor(if (idx == 0) Color.parseColor("#10B981") else Color.parseColor("#E2E8F0"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val timeTv = TextView(activity).apply {
                    text = "记录时间: ${sdf.format(Date(r.movedAt))}" + if (r.note.isNotBlank()) " · ${r.note}" else ""
                    textSize = 11f
                    setTextColor(Color.parseColor("#94A3B8"))
                    setPadding(0, 4, 0, 0)
                }

                row.addView(titleTv)
                row.addView(timeTv)

                if (idx < records.size - 1) {
                    val divider = View(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        ).apply { topMargin = 12 }
                        setBackgroundColor(Color.parseColor("#1E293B"))
                    }
                    row.addView(divider)
                }

                container.addView(row)
            }
        }

        binding.btnCloseHistory.applyPressScaleAnimation(0.92f)
        binding.btnHistoryDone.applyPressScaleAnimation(0.92f)

        binding.btnCloseHistory.setOnClickListener { dialog.dismiss() }
        binding.btnHistoryDone.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
