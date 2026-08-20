package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogAiConciergeBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🤖 AI 资产智能管家与全屋收纳空间优化顾问 (AI Storage & Asset Concierge)
 * 1. 🏥 全屋收纳空间科学体检：空间负荷均衡度、存储环境安全风险排查（高温/潮湿/易遗忘）与流浪物品归位
 * 2. 🔍 离线自然语言多维精准寻物与平面图一键穿梭高亮
 * 3. 💰 资产财务穿透统计与累计折旧现值核算
 * 4. ⏳ 临期风险、耗材补货、设备维保与沉睡闲置回血全维智能穿透
 */
object AiConciergeHelper {

    private const val TAG = "AiConciergeHelper"

    fun showConciergeDialog(activity: Activity, store: DataStore) {
        val binding = DialogAiConciergeBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.btnCloseConcierge.applyPressScaleAnimation(0.92f)
        binding.btnCloseConcierge.setOnClickListener { dialog.dismiss() }

        fun executeQuery(queryText: String) {
            val q = queryText.trim()
            if (q.isEmpty()) return
            binding.etConciergeQuery.setText(q)
            binding.tvConciergeGreeting.visibility = View.GONE
            binding.layoutResultCards.visibility = View.VISIBLE
            binding.layoutResultCards.removeAllViews()

            renderAnswer(activity, store, q, binding.layoutResultCards) {
                dialog.dismiss()
            }
        }

        binding.btnSendQuery.applyPressScaleAnimation(0.92f)
        binding.btnSendQuery.setOnClickListener {
            val q = binding.etConciergeQuery.text.toString().trim()
            if (q.isNotEmpty()) {
                executeQuery(q)
            } else {
                Toast.makeText(activity, "请输入提问或寻物内容", Toast.LENGTH_SHORT).show()
            }
        }

        binding.chipPromptFind.setOnClickListener { executeQuery("我的相机和充电器放在哪了？") }
        binding.chipPromptWorth.setOnClickListener { executeQuery("数码类设备总共有多少件，总价值多少？") }
        binding.chipPromptExpiring.setOnClickListener { executeQuery("最近有哪些物品快要过期了？") }
        binding.chipPromptStock.setOnClickListener { executeQuery("哪些生活耗材快要用完了需要补货？") }
        binding.chipPromptSpaceAudit.setOnClickListener { executeQuery("请对全屋收纳空间进行健康体检与优化诊断") }
        binding.chipPromptMaint.setOnClickListener { executeQuery("家里有哪些设备近期需要清洗、换滤芯或维保？") }
        binding.chipPromptDeclutter.setOnClickListener { executeQuery("库内有哪些沉睡闲置物品可以转卖回血？") }

        dialog.show()
    }

    private fun renderAnswer(
        activity: Activity,
        store: DataStore,
        query: String,
        container: LinearLayout,
        onDismiss: () -> Unit
    ) {
        val all = store.loadAll()
        val activeEntries = all.filter { !it.isRetired && !it.isSubscription }
        val qLower = query.lowercase()

        val answerHeader = TextView(activity).apply {
            text = "💬 针对问题「$query」的智能分析："
            textSize = 12f
            setTextColor(ContextCompat.getColor(activity, R.color.primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8.dpToPx(activity))
        }
        container.addView(answerHeader)

        // 1. 🏥 全屋空间收纳体检与优化诊断
        if (qLower.contains("体检") || qLower.contains("空间") || qLower.contains("收纳") || qLower.contains("负荷") || qLower.contains("诊断")) {
            val totalActive = activeEntries.size
            if (totalActive == 0) {
                container.addView(createInfoCard(activity, "🏥 全屋空间体检报告", "库内暂无在库物品，请先添加资产或空间房间！"))
                return
            }

            // 空间负荷分布
            val roomGroups = activeEntries.groupBy { if (it.roomName.isNotBlank()) it.roomName else "未分配房间" }
            val maxRoom = roomGroups.maxByOrNull { it.value.size }
            val homelessCount = activeEntries.count { it.roomName.isBlank() }

            // 环境存储风险排查
            val medicineRisks = activeEntries.filter {
                (it.category.contains("药") || it.category.contains("保健") || it.category.contains("医")) &&
                (it.roomName.contains("阳台") || it.roomName.contains("厨房") || it.roomName.contains("卫") || it.roomName.contains("浴室"))
            }

            val foodRisks = activeEntries.filter {
                (it.category.contains("食品") || it.category.contains("生鲜") || it.category.contains("零食")) &&
                it.roomName.isNotBlank() &&
                !it.roomName.contains("厨房") && !it.roomName.contains("餐厅") && !it.roomName.contains("冰箱")
            }

            val unpinnedImportant = activeEntries.filter { it.isImportant && it.pinX < 0f }

            val sb = StringBuilder()
            sb.append("📊 【空间负荷评估】：\n")
            if (maxRoom != null && totalActive > 5) {
                val maxPct = (maxRoom.value.size * 100 / totalActive)
                if (maxPct >= 45) {
                    sb.append("• ⚠️ 【${maxRoom.key}】收纳了全家 $maxPct% 的物品 (${maxRoom.value.size}件)，负荷过重，建议将部分物品分流至储物间或书房。\n")
                } else {
                    sb.append("• 🟢 全屋空间负荷分布均衡，最密集区域为【${maxRoom.key}】(${maxRoom.value.size}件，占比 $maxPct%)。\n")
                }
            } else {
                sb.append("• 🟢 空间在库密度适中，运转良好。\n")
            }

            sb.append("\n🛡️ 【存储环境安全排查】：\n")
            if (medicineRisks.isNotEmpty()) {
                sb.append("• ⚠️ 发现 ${medicineRisks.size} 种药品（如 ${medicineRisks.take(2).joinToString("、") { it.brand }}）存放在阳台/厨房/卫生间等高温潮湿区，建议移至阴凉干燥处！\n")
            }
            if (foodRisks.isNotEmpty()) {
                sb.append("• 🥫 发现 ${foodRisks.size} 种食品存放在非厨房/餐厅区域，注意先进先出防临期！\n")
            }
            if (unpinnedImportant.isNotEmpty()) {
                sb.append("• 📍 发现 ${unpinnedImportant.size} 件贵重证件/资产未在平面图上精准标定图钉坐标。\n")
            }
            if (medicineRisks.isEmpty() && foodRisks.isEmpty() && unpinnedImportant.isEmpty()) {
                sb.append("• 🎉 太棒了！未排查到高温受潮、食品易遗忘等环境存放风险。\n")
            }

            if (homelessCount > 0) {
                sb.append("\n🏠 【流浪物品提醒】：\n• 共有 $homelessCount 件物品暂未标定具体收纳房间，建议前往平面图归位。")
            }

            val auditCard = createInfoCard(activity, "🏥 全屋收纳空间健康体检报告", sb.toString().trimEnd())
            auditCard.setOnClickListener {
                onDismiss()
                FloorPlanDialog.show(activity, store, isSelectMode = false)
            }
            container.addView(auditCard)
            return
        }

        // 2. 🛠️ 设备定期维保与排期穿透
        if (qLower.contains("维保") || qLower.contains("保养") || qLower.contains("滤芯") || qLower.contains("年检") || qLower.contains("换电")) {
            val maintained = activeEntries.filter { it.isMaintenanceEnabled() }
            if (maintained.isEmpty()) {
                val card = createInfoCard(
                    activity,
                    "🛠️ 设备维保日历排期",
                    "当前暂未为净水器、空调、私家车等设备配置维保周期。\n\n点击此处可直接打开【🛠️ 全家资产维保日历舱】套用常用模板！"
                )
                card.setOnClickListener {
                    onDismiss()
                    MaintenanceManagerDialog.show(activity, store)
                }
                container.addView(card)
            } else {
                val overdue = maintained.filter { it.getMaintenanceRemainingDays() < 0 }
                val upcoming = maintained.filter { it.getMaintenanceRemainingDays() in 0..30 }
                val sb = StringBuilder()
                sb.append("• 维保大盘：🔴 ${overdue.size}项超期 | 🟡 ${upcoming.size}项临期 | 🟢 ${maintained.size - overdue.size - upcoming.size}项正常\n\n")
                if (overdue.isNotEmpty()) {
                    sb.append("🔴 超期待维保：\n")
                    for (item in overdue.take(3)) {
                        sb.append("• ${item.brand}: 已超期 ${Math.abs(item.getMaintenanceRemainingDays())} 天 (备忘: ${item.maintenanceNotes.ifBlank { "定期养护" }})\n")
                    }
                }
                if (upcoming.isNotEmpty()) {
                    sb.append("\n🟡 30天内临期维保：\n")
                    for (item in upcoming.take(3)) {
                        sb.append("• ${item.brand}: 还剩 ${item.getMaintenanceRemainingDays()} 天 (周期每${item.maintenanceIntervalMonths}个月)\n")
                    }
                }
                sb.append("\n👉 点击此处可直接打开【🛠️ 全家资产维保日历舱】一键打卡推算！")
                val card = createInfoCard(activity, "🛠️ 全家耐用设备维保排期穿透", sb.toString().trimEnd())
                card.setOnClickListener {
                    onDismiss()
                    MaintenanceManagerDialog.show(activity, store)
                }
                container.addView(card)
            }
            return
        }

        // 3. ♻️ 沉睡闲置与出清回血建议
        if (qLower.contains("闲置") || qLower.contains("出清") || qLower.contains("回血") || qLower.contains("断舍离") || qLower.contains("转卖")) {
            val nowMs = System.currentTimeMillis()
            val idleItems = activeEntries.filter { item ->
                val isUncheckedLong = (item.getDaysOwned() > 180 && (nowMs - item.lastCheckedAt > 180L * 24 * 60 * 60 * 1000))
                val isExpiringSoon = (item.assetType == "expiring" && item.expiryDate > 0 && item.expiryDate - nowMs < 15L * 24 * 60 * 60 * 1000)
                isUncheckedLong || isExpiringSoon
            }

            if (idleItems.isEmpty()) {
                container.addView(createInfoCard(activity, "🎉 资产流转极其健康", "库内暂无超过 180 天未打卡或临期闲置物品，断舍离健康度 100 分！"))
            } else {
                val totalEst = idleItems.sumOf { ResaleCopilotHelper.generateListing(it).fastSellPrice }
                val sb = StringBuilder()
                sb.append("• 发现 ${idleItems.size} 件沉睡闲置资产，预计可回血 ¥${String.format(Locale.getDefault(), "%.2f", totalEst)}\n\n")
                for (item in idleItems.take(4)) {
                    val draft = ResaleCopilotHelper.generateListing(item)
                    sb.append("• ${item.brand}: 已持有 ${item.getDaysOwned()} 天 · 参考秒出价 ¥${String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice)}\n")
                }
                sb.append("\n👉 点击此处直达【♻️ 闲置断舍离与回血决策舱】，一键生成 AI 闲鱼文案或挂售变现！")
                val card = createInfoCard(activity, "♻️ 沉睡闲置与回血决策建议", sb.toString().trimEnd())
                card.setOnClickListener {
                    onDismiss()
                    ResaleCopilotHelper.showDeclutterCabinDialog(activity, store)
                }
                container.addView(card)
            }
            return
        }

        // 4. 🎒 场景装备套装穿透
        if (qLower.contains("套装") || qLower.contains("出行") || qLower.contains("行李") || qLower.contains("出差") || qLower.contains("露营")) {
            val kits = KitManager.getAllKits(activity)
            val sb = StringBuilder()
            sb.append("• 当前已纳管 ${kits.size} 套场景装备包：\n\n")
            for (k in kits.take(4)) {
                sb.append("• ${k.icon} ${k.name}: 收录 ${k.itemIds.size} 件装备\n")
            }
            sb.append("\n👉 点击此处打开【🎒 场景化装备套装】，一键开启去程装箱与返程归巢物归原位！")
            val card = createInfoCard(activity, "🎒 场景化装备套装大盘", sb.toString().trimEnd())
            card.setOnClickListener {
                onDismiss()
                KitManager.showKitListDialog(activity, store)
            }
            container.addView(card)
            return
        }

        // 5. 💰 资产价值深度穿透
        if (qLower.contains("多少钱") || qLower.contains("总值") || qLower.contains("价值") || qLower.contains("统计")) {
            val targetCat = store.getCategories().firstOrNull { qLower.contains(it.lowercase()) }
            val filtered = if (targetCat != null) all.filter { it.category == targetCat } else all
            val totalQty = filtered.sumOf { it.qty }
            val totalWorth = filtered.sumOf { it.price * it.qty }
            val curWorth = filtered.sumOf { (if (it.currentValuation > 0) it.currentValuation else it.price) * it.qty }

            val card = createInfoCard(
                activity,
                "💰 资产价值深度穿透报告",
                "• 筛选范围：${targetCat ?: "全库所有资产"}\n" +
                "• 物品总量：${filtered.size} 种，共 $totalQty 件\n" +
                "• 原始购入总值：¥${String.format(Locale.US, "%.2f", totalWorth)}\n" +
                "• 当前评估现值：¥${String.format(Locale.US, "%.2f", curWorth)}\n" +
                "• 折旧累计额：¥${String.format(Locale.US, "%.2f", (totalWorth - curWorth).coerceAtLeast(0.0))}"
            )
            container.addView(card)
            return
        }

        // 6. ⏳ 临期物品盘点
        if (qLower.contains("过期") || qLower.contains("临期") || qLower.contains("保质期")) {
            val now = System.currentTimeMillis()
            val expiring = all.filter { it.expiryDate > 0L }.sortedBy { it.expiryDate }

            if (expiring.isEmpty()) {
                container.addView(createInfoCard(activity, "🎉 暂无临期风险", "您的物品库中未发现设置保质期或已过期的物品。"))
            } else {
                val sb = StringBuilder()
                for (item in expiring.take(6)) {
                    val days = ((item.expiryDate - now) / (1000L * 60 * 60 * 24)).toInt()
                    val statusStr = when {
                        days < 0 -> "🔴 已过期 ${-days} 天"
                        days == 0 -> "⚠️ 今天到期！"
                        days <= 30 -> "🟡 剩余 $days 天"
                        else -> "🟢 剩余 $days 天"
                    }
                    sb.append("• ${item.brand}: $statusStr (位置: ${item.location.ifBlank { "未分配" }})\n")
                }
                container.addView(createInfoCard(activity, "⏳ 临期/过期物品追踪", sb.toString().trimEnd()))
            }
            return
        }

        // 7. 🛒 耗材补货
        if (qLower.contains("补货") || qLower.contains("用完") || qLower.contains("耗材") || qLower.contains("库存")) {
            val lowStock = all.filter { it.assetType == "consumable" && it.qty <= 1 }
            if (lowStock.isEmpty()) {
                container.addView(createInfoCard(activity, "✅ 耗材库存健康", "当前所有生活消耗品库存均在安全水位之上。"))
            } else {
                val sb = StringBuilder()
                for (item in lowStock) {
                    sb.append("• ${item.brand}: 仅剩 ${item.qty} ${item.unit} (建议尽快补货)\n")
                }
                val card = createInfoCard(activity, "🛒 需补货清单建议", sb.toString().trimEnd())
                container.addView(card)
            }
            return
        }

        // 8. 默认：自然语言关键词智能匹配寻物
        val keywords = query.replace("我的", "")
            .replace("放在哪", "")
            .replace("在哪", "")
            .replace("找", "")
            .replace("了", "")
            .replace("？", "")
            .replace("?", "")
            .split(" ", "和", "跟", "与", "、")
            .filter { it.isNotBlank() }

        val matches = all.filter { entry ->
            keywords.any { kw ->
                entry.brand.contains(kw, ignoreCase = true) ||
                entry.notes.contains(kw, ignoreCase = true) ||
                entry.category.contains(kw, ignoreCase = true) ||
                entry.location.contains(kw, ignoreCase = true)
            }
        }

        if (matches.isEmpty()) {
            val card = createInfoCard(
                activity,
                "🔍 未检索到完全匹配的物品",
                "建议尝试输入更简短的物品名称（如“相机”、“牛奶”），或前往首页顶栏搜索。"
            )
            container.addView(card)
        } else {
            val titleView = TextView(activity).apply {
                text = "🎯 找到 ${matches.size} 件相关物品位置："
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 4.dpToPx(activity), 0, 6.dpToPx(activity))
            }
            container.addView(titleView)

            for (m in matches.take(5)) {
                val locText = if (m.roomName.isNotBlank() || m.location.isNotBlank()) {
                    "${if (m.roomName.isNotBlank()) "🏠 " + m.roomName + " · " else ""}${m.location.ifBlank { "默认空间" }}"
                } else {
                    "暂未标定存放空间"
                }

                val hasCoords = m.pinX >= 0 && m.pinY >= 0
                val desc = "• 分类: ${m.category} | 数量: ${m.qty} ${m.unit}\n" +
                           "• 存放位置: $locText\n" +
                           (if (m.notes.isNotBlank()) "• 备注: ${m.notes}\n" else "") +
                           (if (hasCoords) "📍 已在空间平面图精准标定坐标 (点击直达穿梭)" else "")

                val itemCard = createInfoCard(activity, "📦 ${m.brand}", desc.trimEnd())
                if (hasCoords) {
                    itemCard.setOnClickListener {
                        onDismiss()
                        FloorPlanDialog.show(activity, store, isSelectMode = false, targetEntry = m)
                    }
                }
                container.addView(itemCard)
            }
        }
    }

    private fun createInfoCard(context: Context, title: String, content: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_dialog_card)
            setPadding(12.dpToPx(context), 10.dpToPx(context), 12.dpToPx(context), 10.dpToPx(context))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx(context)
            }
            layoutParams = lp
            applyPressScaleAnimation(0.95f)

            val tvTitle = TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 4.dpToPx(context))
            }

            val tvContent = TextView(context).apply {
                text = content
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setLineSpacing(3f.dpToPx(context).toFloat(), 1f)
            }

            addView(tvTitle)
            addView(tvContent)
        }
    }

    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
    private fun Float.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
