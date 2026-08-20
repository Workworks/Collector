package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
 * 🤖 AI 资产智能管家引擎 (AI Asset Concierge)
 * 1. 自然语言寻物与多维精准位置高亮
 * 2. 资产财务穿透统计与折旧总值计算
 * 3. 临期风险与耗材低库存动态预警
 * 4. 出行装备与场景组合推荐
 */
object AiConciergeHelper {

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
        val qLower = query.lowercase()

        val answerHeader = TextView(activity).apply {
            text = "💬 针对问题「$query」的智能分析："
            textSize = 12f
            setTextColor(ContextCompat.getColor(activity, R.color.primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8.dpToPx(activity))
        }
        container.addView(answerHeader)

        if (qLower.contains("多少钱") || qLower.contains("总值") || qLower.contains("价值") || qLower.contains("统计")) {
            // 资产价值分析
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

        if (qLower.contains("过期") || qLower.contains("临期") || qLower.contains("保质期")) {
            // 临期物品盘点
            val now = System.currentTimeMillis()
            val expiring = all.filter { it.expiryDate > 0L }
                .sortedBy { it.expiryDate }

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

        if (qLower.contains("补货") || qLower.contains("用完") || qLower.contains("耗材") || qLower.contains("库存")) {
            // 耗材低库存
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

        // 默认：自然语言关键词智能匹配寻物
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
                           (if (hasCoords) "📍 已在空间平面图精准标定坐标" else "")

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
