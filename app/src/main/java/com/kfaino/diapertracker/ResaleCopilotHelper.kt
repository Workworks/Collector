package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * 闲鱼 / 转转退役转卖 AI 智能文案与行情定价助手 (AI Xianyu Listing Copilot)
 * - 结合物品品牌、购入原价、拥有天数、折旧曲线与成色自动生成高转化商品文案
 * - 智能估算秒出参考价与合理挂牌价
 */
object ResaleCopilotHelper {

    data class ListingDraft(
        val title: String,
        val description: String,
        val fastSellPrice: Double,
        val suggestPrice: Double
    )

    fun generateListing(entry: Entry, condition: String = "95新", reason: String = "升级换新出闲置"): ListingDraft {
        val days = entry.getDaysOwned()
        val origPrice = entry.price * entry.qty
        val dailyCost = entry.getDailyCost()

        // 折旧估算行情价
        val residualRate = when {
            entry.category == "数码" -> (1.0 - (days / 1000.0) * 0.5).coerceIn(0.2, 0.85)
            entry.category == "零食" || entry.category == "耗材" -> 0.4
            else -> (1.0 - (days / 1500.0) * 0.6).coerceIn(0.15, 0.80)
        }

        val fastSellPrice = (origPrice * residualRate).coerceAtLeast(10.0)
        val suggestPrice = (fastSellPrice * 1.15).coerceAtLeast(fastSellPrice)

        val title = "【个人自用一手$condition】${entry.brand} (正品保证/成色极佳/爽快包邮)"

        val desc = buildString {
            append("【个人闲置转让 · ${entry.brand}】\n\n")
            append("1️⃣ 来源背景：个人一手购入自用，原价 ¥${String.format(Locale.getDefault(), "%.2f", origPrice)}，累计使用约 $days 天，非常爱惜。\n")
            append("2️⃣ 成色细节：整体成色 $condition，功能完好无任何暗病，按键与接口灵敏。\n")
            append("3️⃣ 闲置原因：因 $reason，现忍痛低价出给有需要的朋友回血。\n")
            append("4️⃣ 交易保障：个人自用卖家非二手贩子，诚信交易，支持验货，同城可自提或爽快包邮！\n\n")
            append("#闲置出 #个人自用 #${entry.category} #${entry.brand}")
        }

        return ListingDraft(
            title = title,
            description = desc,
            fastSellPrice = fastSellPrice,
            suggestPrice = suggestPrice
        )
    }

    /** 弹出闲鱼智能文案与定价弹窗 */
    fun showListingCopilotDialog(activity: Activity, entry: Entry, onPriceSelected: (Double) -> Unit) {
        val draft = generateListing(entry)

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 28)
        }

        val titleTv = TextView(activity).apply {
            text = "🤖 AI 闲鱼/转转转卖文案助手"
            textSize = 17f
            paint.isFakeBoldText = true
            setPadding(0, 0, 0, 12)
        }

        val priceAdviceTv = TextView(activity).apply {
            text = "💰 行情估价：秒出价 ¥${String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice)}  |  建议挂牌价 ¥${String.format(Locale.getDefault(), "%.2f", draft.suggestPrice)}"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#10B981"))
            paint.isFakeBoldText = true
            setPadding(0, 0, 0, 16)
        }

        val editTitle = EditText(activity).apply {
            setText(draft.title)
            textSize = 14f
            setPadding(20, 16, 20, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#1E293B"))
        }

        val editDesc = EditText(activity).apply {
            setText(draft.description)
            textSize = 13f
            setLines(7)
            setPadding(20, 16, 20, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#1E293B"))
        }

        dialogView.addView(titleTv)
        dialogView.addView(priceAdviceTv)
        dialogView.addView(TextView(activity).apply { text = "📋 推荐标题 (点击可修改):"; textSize = 12f; setPadding(0, 0, 0, 4) })
        dialogView.addView(editTitle)
        dialogView.addView(TextView(activity).apply { text = "📝 推荐商品描述详情:"; textSize = 12f; setPadding(0, 12, 0, 4) })
        dialogView.addView(editDesc)

        MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setPositiveButton("📋 复制全部文案并采纳秒出价") { _, _ ->
                val fullText = "${editTitle.text}\n\n${editDesc.text}"
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Xianyu Listing", fullText))
                Toast.makeText(activity, "🎉 闲鱼文案已复制！可直接粘贴发布", Toast.LENGTH_SHORT).show()
                onPriceSelected(draft.fastSellPrice)
            }
            .setNegativeButton("仅关闭", null)
            .show()
    }
}
