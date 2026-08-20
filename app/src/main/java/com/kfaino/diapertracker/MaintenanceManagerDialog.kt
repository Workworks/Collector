package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🛠️ 全家耐用资产定期维保与年检日历舱 (Asset Maintenance Hub)
 * 1. 净水器、空调、私家车、智能门锁、扫地机等全家关键设备周期性养护
 * 2. 🔴 超期、🟡 30天内临期、🟢 健康运转中三色动态透视大盘
 * 3. 🛠️ 一键维保打卡登记（记录费用与耗材型号）与下次排期自动推算
 * 4. ✨ 常用家庭维保模板（6 大高频预设）一键极速绑定
 */
object MaintenanceManagerDialog {

    private const val TAG = "MaintenanceManagerDialog"

    data class MaintenancePreset(
        val name: String,
        val icon: String,
        val intervalMonths: Int,
        val defaultNotes: String
    )

    private val PRESETS = listOf(
        MaintenancePreset("净水器滤芯更换", "🚰", 6, "更换复合/RO反渗透滤芯，冲洗水路"),
        MaintenancePreset("空调深度拆洗除菌", "❄️", 6, "拆洗滤网、蒸发器除菌清洗"),
        MaintenancePreset("汽车定期保养/年检", "🚗", 12, "更换机油机滤、检查胎压刹车片与年检"),
        MaintenancePreset("智能门锁换电池", "🔐", 12, "更换5号碱性电池，防止电量耗尽锁定"),
        MaintenancePreset("扫地机/吸尘器耗材", "🧹", 3, "清洗/更换 HEPA 滤网与主刷"),
        MaintenancePreset("真皮沙发/皮具保养", "🛋️", 12, "真皮滋养膏涂抹防干裂")
    )

    fun show(activity: Activity, store: DataStore, onDataChanged: (() -> Unit)? = null) {
        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(16.dpToPx(activity), 16.dpToPx(activity), 16.dpToPx(activity), 16.dpToPx(activity))
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        fun refreshContent() {
            dialogView.removeAllViews()
            val allEntries = store.loadAll()
            val activeEntries = allEntries.filter { !it.isRetired && !it.isSubscription }
            val maintainedEntries = activeEntries.filter { it.isMaintenanceEnabled() }

            val overdueList = maintainedEntries.filter { it.getMaintenanceRemainingDays() < 0 }
            val upcomingList = maintainedEntries.filter { it.getMaintenanceRemainingDays() in 0..30 }
            val healthyList = maintainedEntries.filter { it.getMaintenanceRemainingDays() > 30 }

            // 1. 顶栏标题与操作
            val topBar = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 10.dpToPx(activity))
            }

            val titleTv = TextView(activity).apply {
                text = "🛠️ 全家资产维保日历舱"
                textSize = 17f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnClose = ImageView(activity).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(6.dpToPx(activity), 6.dpToPx(activity), 6.dpToPx(activity), 6.dpToPx(activity))
                layoutParams = LinearLayout.LayoutParams(32.dpToPx(activity), 32.dpToPx(activity))
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { dialog.dismiss() }
            }

            topBar.addView(titleTv)
            topBar.addView(btnClose)
            dialogView.addView(topBar)

            // 2. 维保健康度大盘指标卡片
            val overviewCard = MaterialCardView(activity).apply {
                radius = 14.dpToPx(activity).toFloat()
                cardElevation = 0f
                strokeWidth = 1.dpToPx(activity)
                setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = 10.dpToPx(activity)
                layoutParams = lp
            }

            val overviewLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dpToPx(activity), 10.dpToPx(activity), 14.dpToPx(activity), 10.dpToPx(activity))
            }

            val metricsRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val statusSummaryTv = TextView(activity).apply {
                text = "🔴 ${overdueList.size}项超期  🟡 ${upcomingList.size}项临期  🟢 ${healthyList.size}项正常"
                textSize = 13f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val totalCountBadge = TextView(activity).apply {
                text = "共纳管 ${maintainedEntries.size} 件设备"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                paint.isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_stock_healthy)
                setPadding(8.dpToPx(activity), 3.dpToPx(activity), 8.dpToPx(activity), 3.dpToPx(activity))
            }

            metricsRow.addView(statusSummaryTv)
            metricsRow.addView(totalCountBadge)
            overviewLayout.addView(metricsRow)
            overviewCard.addView(overviewLayout)
            dialogView.addView(overviewCard)

            // 3. 维保条目列表容器
            val scroll = ScrollView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 280.dpToPx(activity))
                isFillViewport = true
            }

            val listLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
            }

            if (maintainedEntries.isEmpty()) {
                val emptyTv = TextView(activity).apply {
                    text = "💡 暂未为家庭设备配置定期维保计划。\n点击下方「✨ 常用维保模板」或在物品编辑中开启定期维保，让净水器换芯、空调清洗不再遗忘！"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setPadding(16.dpToPx(activity), 24.dpToPx(activity), 16.dpToPx(activity), 24.dpToPx(activity))
                }
                listLayout.addView(emptyTv)
            } else {
                val sortedList = maintainedEntries.sortedBy { it.getMaintenanceRemainingDays() }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (item in sortedList) {
                    val remainingDays = item.getMaintenanceRemainingDays()
                    val nextDate = item.getNextMaintenanceDate()
                    val nextDateStr = if (nextDate > 0) sdf.format(Date(nextDate)) else "未推算"

                    val itemCard = MaterialCardView(activity).apply {
                        radius = 12.dpToPx(activity).toFloat()
                        cardElevation = 0f
                        strokeWidth = 1.dpToPx(activity)
                        setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                        setCardBackgroundColor(ContextCompat.getColor(context, R.color.input_bg))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.bottomMargin = 8.dpToPx(activity)
                        layoutParams = lp
                    }

                    val rowLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(12.dpToPx(activity), 10.dpToPx(activity), 12.dpToPx(activity), 10.dpToPx(activity))
                    }

                    val headerRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val nameTv = TextView(activity).apply {
                        text = "🛠️ ${item.brand}"
                        textSize = 13f
                        paint.isFakeBoldText = true
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val statusBadge = TextView(activity).apply {
                        when {
                            remainingDays < 0 -> {
                                text = "🔴 已超期 ${Math.abs(remainingDays)} 天"
                                setTextColor(ContextCompat.getColor(context, R.color.danger))
                                setBackgroundResource(R.drawable.bg_stock_empty)
                            }
                            remainingDays in 0..30 -> {
                                text = "🟡 剩 $remainingDays 天"
                                setTextColor(ContextCompat.getColor(context, R.color.accent_dark))
                                setBackgroundResource(R.drawable.bg_stock_low)
                            }
                            else -> {
                                text = "🟢 剩 $remainingDays 天"
                                setTextColor(ContextCompat.getColor(context, R.color.stock_healthy_text))
                                setBackgroundResource(R.drawable.bg_stock_healthy)
                            }
                        }
                        textSize = 11f
                        paint.isFakeBoldText = true
                        setPadding(6.dpToPx(activity), 2.dpToPx(activity), 6.dpToPx(activity), 2.dpToPx(activity))
                    }

                    headerRow.addView(nameTv)
                    headerRow.addView(statusBadge)
                    rowLayout.addView(headerRow)

                    val cycleTv = TextView(activity).apply {
                        text = "周期: 每 ${item.maintenanceIntervalMonths} 个月 · 下次排期: $nextDateStr (📍 ${item.houseName} / ${item.roomName.ifBlank { "未指定" }})"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setPadding(0, 3.dpToPx(activity), 0, 0)
                    }
                    rowLayout.addView(cycleTv)

                    if (item.maintenanceNotes.isNotBlank()) {
                        val notesTv = TextView(activity).apply {
                            text = "备忘: ${item.maintenanceNotes}"
                            textSize = 11f
                            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        }
                        rowLayout.addView(notesTv)
                    }

                    // 操作栏 (打卡完成维保 / 修改周期)
                    val actionRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 6.dpToPx(activity), 0, 0)
                    }

                    val btnCheckin = TextView(activity).apply {
                        text = "🛠️ 完成本次维保打卡"
                        textSize = 11f
                        paint.isFakeBoldText = true
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                        setBackgroundResource(R.drawable.bg_btn_custom_add)
                        setPadding(8.dpToPx(activity), 4.dpToPx(activity), 8.dpToPx(activity), 4.dpToPx(activity))
                        applyPressScaleAnimation(0.92f)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = 6.dpToPx(activity)
                        }
                        layoutParams = lp
                        setOnClickListener {
                            promptCompleteMaintenance(activity, store, item) {
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    val btnEditCycle = TextView(activity).apply {
                        text = "⚙️ 修改周期"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setBackgroundResource(R.drawable.bg_chip_inactive)
                        setPadding(8.dpToPx(activity), 4.dpToPx(activity), 8.dpToPx(activity), 4.dpToPx(activity))
                        applyPressScaleAnimation(0.92f)
                        setOnClickListener {
                            promptConfigMaintenance(activity, store, item) {
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    actionRow.addView(btnCheckin)
                    actionRow.addView(btnEditCycle)
                    rowLayout.addView(actionRow)

                    itemCard.addView(rowLayout)
                    listLayout.addView(itemCard)
                }
            }

            scroll.addView(listLayout)
            dialogView.addView(scroll)

            // 4. 底部常用维保模板预设与添加按钮组
            val bottomBar = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10.dpToPx(activity), 0, 0)
            }

            val btnPresets = TextView(activity).apply {
                text = "✨ 常用维保模板预设"
                textSize = 12f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(0, 10.dpToPx(activity), 0, 10.dpToPx(activity))
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 6.dpToPx(activity)
                }
                layoutParams = lp
                applyPressScaleAnimation(0.92f)
                setOnClickListener {
                    showPresetsSelector(activity, store, activeEntries) {
                        refreshContent()
                        onDataChanged?.invoke()
                    }
                }
            }

            val btnAddForEntry = TextView(activity).apply {
                text = "➕ 为物品添加维保"
                textSize = 12f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setBackgroundResource(R.drawable.bg_btn_primary)
                setPadding(0, 10.dpToPx(activity), 0, 10.dpToPx(activity))
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 6.dpToPx(activity)
                }
                layoutParams = lp
                applyPressScaleAnimation(0.92f)
                setOnClickListener {
                    showPickEntryForMaintenance(activity, store, activeEntries) {
                        refreshContent()
                        onDataChanged?.invoke()
                    }
                }
            }

            bottomBar.addView(btnPresets)
            bottomBar.addView(btnAddForEntry)
            dialogView.addView(bottomBar)
        }

        refreshContent()
        dialog.show()
    }

    /** 弹出完成维保打卡弹窗 */
    private fun promptCompleteMaintenance(
        activity: Activity,
        store: DataStore,
        item: Entry,
        onDone: () -> Unit
    ) {
        ModernDialogHelper.showInputDialog(
            context = activity,
            title = "完成【${item.brand}】维保打卡",
            subtitle = "输入本次维保花费金额 (元，如耗材/服务费，0表示无花费)",
            hint = "如 180",
            defaultValue = "0",
            emoji = "🛠️"
        ) { costStr ->
            val now = System.currentTimeMillis()
            val all = store.loadAll().toMutableList()
            val idx = all.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                all[idx] = item.copy(
                    lastMaintainedAt = now,
                    lastCheckedAt = now
                )
                store.saveAll(all)
                Toast.makeText(activity, "🎉 已成功完成【${item.brand}】维保打卡！下次排期已自动推算。", Toast.LENGTH_LONG).show()
                onDone()
            }
        }
    }

    /** 弹出维保周期配置弹窗 */
    private fun promptConfigMaintenance(
        activity: Activity,
        store: DataStore,
        item: Entry,
        onDone: () -> Unit
    ) {
        val cycleOptions = listOf("每 3 个月 (季度维保)", "每 6 个月 (半年维保)", "每 12 个月 (年度维保)", "每 24 个月 (两年维保)", "🚫 关闭维保提醒")
        val defaultIdx = when (item.maintenanceIntervalMonths) {
            3 -> 0
            6 -> 1
            12 -> 2
            24 -> 3
            else -> 1
        }

        ModernDialogHelper.showSingleChoiceDialog(
            context = activity,
            title = "配置【${item.brand}】维保周期",
            emoji = "⚙️",
            options = cycleOptions,
            selectedIndex = defaultIdx
        ) { which, _ ->
            val months = when (which) {
                0 -> 3
                1 -> 6
                2 -> 12
                3 -> 24
                else -> 0
            }
            val all = store.loadAll().toMutableList()
            val idx = all.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                all[idx] = item.copy(
                    maintenanceIntervalMonths = months,
                    lastMaintainedAt = if (months > 0 && item.lastMaintainedAt == 0L) System.currentTimeMillis() else item.lastMaintainedAt
                )
                store.saveAll(all)
                Toast.makeText(activity, if (months > 0) "🎉 已成功设置【${item.brand}】维保周期为每 $months 个月！" else "已关闭维保提醒", Toast.LENGTH_SHORT).show()
                onDone()
            }
        }
    }

    /** 常用维保模板快速套用 */
    private fun showPresetsSelector(
        activity: Activity,
        store: DataStore,
        activeEntries: List<Entry>,
        onDone: () -> Unit
    ) {
        val presetNames = PRESETS.map { "${it.icon} ${it.name} (每${it.intervalMonths}个月)" }
        ModernDialogHelper.showSingleChoiceDialog(
            context = activity,
            title = "选择常用家庭维保模板",
            emoji = "✨",
            options = presetNames,
            selectedIndex = 0
        ) { which, _ ->
            val preset = PRESETS[which]
            // 选择要绑定的已有物品
            val entryNames = activeEntries.map { "📦 ${it.brand} (📍 ${it.location.ifBlank { "未指定" }})" }
            if (entryNames.isEmpty()) {
                Toast.makeText(activity, "库内暂无物品，请先添加设备！", Toast.LENGTH_SHORT).show()
                return@showSingleChoiceDialog
            }

            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择套用【${preset.name}】的设备",
                emoji = preset.icon,
                options = entryNames,
                selectedIndex = 0
            ) { entryIdx, _ ->
                val targetEntry = activeEntries[entryIdx]
                val all = store.loadAll().toMutableList()
                val idx = all.indexOfFirst { it.id == targetEntry.id }
                if (idx != -1) {
                    all[idx] = targetEntry.copy(
                        maintenanceIntervalMonths = preset.intervalMonths,
                        maintenanceNotes = preset.defaultNotes,
                        lastMaintainedAt = System.currentTimeMillis()
                    )
                    store.saveAll(all)
                    Toast.makeText(activity, "🎉 成功为【${targetEntry.brand}】套用【${preset.name}】维保模板！", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        }
    }

    /** 为已有物品添加维保 */
    private fun showPickEntryForMaintenance(
        activity: Activity,
        store: DataStore,
        activeEntries: List<Entry>,
        onDone: () -> Unit
    ) {
        val unmaintained = activeEntries.filter { !it.isMaintenanceEnabled() }
        if (unmaintained.isEmpty()) {
            Toast.makeText(activity, "库内所有设备均已配置维保计划！", Toast.LENGTH_SHORT).show()
            return
        }

        val names = unmaintained.map { "📦 ${it.brand} (${it.category})" }
        ModernDialogHelper.showSingleChoiceDialog(
            context = activity,
            title = "选择要添加维保计划的设备",
            emoji = "🛠️",
            options = names,
            selectedIndex = 0
        ) { which, _ ->
            val selected = unmaintained[which]
            promptConfigMaintenance(activity, store, selected, onDone)
        }
    }

    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
