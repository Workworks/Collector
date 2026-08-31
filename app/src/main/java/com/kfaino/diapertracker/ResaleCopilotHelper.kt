package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * ♻️ 闲置断舍离与回血决策舱 (Decluttering & Resale Copilot)
 * 1. 沉睡闲置资产智能雷达大盘（预计回血总额、件数与日均成本透视）
 * 2. AI 闲鱼/转转高转化结构化出清文案与行情估价
 * 3. 闭环流转决策：转卖回血（抵扣历史支出）、赠送亲友、环保回收与确认在位
 */
object ResaleCopilotHelper {

    private const val TAG = "ResaleCopilotHelper"

    data class ListingDraft(
        val title: String,
        val description: String,
        val fastSellPrice: Double,
        val suggestPrice: Double
    )

    /** 根据物品生命周期与折旧模型生成高转化二手转卖文案与估价 */
    fun generateListing(entry: Entry, condition: String = "95新", reason: String = "升级换新出闲置"): ListingDraft {
        val days = entry.getDaysOwned().coerceAtLeast(1)
        val origPrice = entry.price * entry.qty

        // 折旧估算行情价
        val residualRate = when {
            entry.category == "数码" -> (1.0 - (days / 1000.0) * 0.5).coerceIn(0.2, 0.85)
            entry.category == "零食" || entry.category == "生鲜" || entry.category == "耗材" -> 0.4
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

    /** 弹出全景「♻️ 闲置断舍离与回血决策舱」 */
    fun showDeclutterCabinDialog(activity: Activity, store: DataStore, onDataChanged: (() -> Unit)? = null) {
        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(activity.dpToPx(16), activity.dpToPx(16), activity.dpToPx(16), activity.dpToPx(16))
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
            val activeNonSubs = allEntries.filter { !it.isRetired && !it.isSubscription }
            val nowMs = System.currentTimeMillis()

            // 筛选沉睡或临期闲置物品 (拥有>180天且未打卡 或 15天内临期)
            val idleItems = activeNonSubs.filter { item ->
                val isUncheckedLong = (item.getDaysOwned() > 180 && (nowMs - item.lastCheckedAt > 180L * 24 * 60 * 60 * 1000))
                val isExpiringSoon = (item.assetType == "expiring" && item.expiryDate > 0 && item.expiryDate - nowMs < 15L * 24 * 60 * 60 * 1000)
                isUncheckedLong || isExpiringSoon
            }

            // 1. 顶栏标题与关闭按钮
            val topBar = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, activity.dpToPx(10))
            }

            val titleTv = TextView(activity).apply {
                text = "♻️ 闲置断舍离与回血决策舱"
                textSize = 17f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnClose = ImageView(activity).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(activity.dpToPx(6), activity.dpToPx(6), activity.dpToPx(6), activity.dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(activity.dpToPx(32), activity.dpToPx(32))
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { dialog.dismiss() }
            }

            topBar.addView(titleTv)
            topBar.addView(btnClose)
            dialogView.addView(topBar)

            // 2. 回血大盘指标卡片
            val totalEstResale = idleItems.sumOf { generateListing(it).fastSellPrice }
            val avgDays = if (idleItems.isNotEmpty()) idleItems.map { it.getDaysOwned() }.average() else 0.0

            val overviewCard = MaterialCardView(activity).apply {
                radius = activity.dpToPx(14).toFloat()
                cardElevation = 0f
                strokeWidth = activity.dpToPx(1)
                setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = activity.dpToPx(12)
                layoutParams = lp
            }

            val overviewLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dpToPx(14), activity.dpToPx(12), activity.dpToPx(14), activity.dpToPx(12))
            }

            val metricRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val resaleTv = TextView(activity).apply {
                text = "💰 预计可回血: ¥${String.format(Locale.getDefault(), "%.2f", totalEstResale)}"
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val countBadge = TextView(activity).apply {
                text = "${idleItems.size} 件沉睡闲置"
                textSize = 12f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.accent_dark))
                setBackgroundResource(R.drawable.bg_stock_low)
                setPadding(activity.dpToPx(8), activity.dpToPx(3), activity.dpToPx(8), activity.dpToPx(3))
            }

            metricRow.addView(resaleTv)
            metricRow.addView(countBadge)
            overviewLayout.addView(metricRow)

            val hintTv = TextView(activity).apply {
                text = "📊 沉睡物品平均持有 ${String.format(Locale.getDefault(), "%.0f", avgDays)} 天 · 日均成本已降至极低，建议挂闲鱼变现回血或赠送亲友减负！"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, activity.dpToPx(4), 0, 0)
            }
            overviewLayout.addView(hintTv)
            overviewCard.addView(overviewLayout)
            dialogView.addView(overviewCard)

            // 3. 闲置物品流转列表
            if (idleItems.isEmpty()) {
                val emptyTv = TextView(activity).apply {
                    text = "🎉 太棒了！库内暂无沉睡超过 180 天或临期闲置物品，资产流转极其健康！"
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.stock_healthy_text))
                    setPadding(activity.dpToPx(16), activity.dpToPx(24), activity.dpToPx(16), activity.dpToPx(24))
                }
                dialogView.addView(emptyTv)
            } else {
                val scroll = ScrollView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dpToPx(340))
                    isFillViewport = true
                }

                val listLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                }

                for (item in idleItems) {
                    val draft = generateListing(item)
                    val card = MaterialCardView(activity).apply {
                        radius = activity.dpToPx(12).toFloat()
                        cardElevation = 0f
                        strokeWidth = activity.dpToPx(1)
                        setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                        setCardBackgroundColor(ContextCompat.getColor(context, R.color.input_bg))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.bottomMargin = activity.dpToPx(8)
                        layoutParams = lp
                    }

                    val rowLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(activity.dpToPx(12), activity.dpToPx(10), activity.dpToPx(12), activity.dpToPx(10))
                    }

                    // 物品信息行 (封面 + 名称 + 原价 + 估价)
                    val infoRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val iconView: View = if (item.photoPath.isNotBlank()) {
                        val bm = ImageVaultHelper.loadSampledBitmap(activity, item.photoPath, 80, 80)
                        if (bm != null) {
                            ImageView(activity).apply {
                                setImageBitmap(bm)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                layoutParams = LinearLayout.LayoutParams(activity.dpToPx(30), activity.dpToPx(30)).apply {
                                    marginEnd = activity.dpToPx(8)
                                }
                            }
                        } else {
                            TextView(activity).apply {
                                text = "📦"
                                textSize = 16f
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    marginEnd = activity.dpToPx(8)
                                }
                            }
                        }
                    } else {
                        TextView(activity).apply {
                            text = "📦"
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                marginEnd = activity.dpToPx(8)
                            }
                        }
                    }

                    val titleLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val nameTv = TextView(activity).apply {
                        text = item.brand
                        textSize = 13f
                        paint.isFakeBoldText = true
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }

                    val detailTv = TextView(activity).apply {
                        text = "原价 ¥${String.format(Locale.getDefault(), "%.2f", item.price * item.qty)} · 已持有 ${item.getDaysOwned()} 天 (日均 ¥${String.format(Locale.getDefault(), "%.2f", item.getDailyCost())}/天)"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    }

                    titleLayout.addView(nameTv)
                    titleLayout.addView(detailTv)

                    val estPriceTv = TextView(activity).apply {
                        text = "估值 ¥${String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice)}"
                        textSize = 12f
                        paint.isFakeBoldText = true
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }

                    infoRow.addView(iconView)
                    infoRow.addView(titleLayout)
                    infoRow.addView(estPriceTv)
                    rowLayout.addView(infoRow)

                    // 决策操作按钮行 (AI文案 / 挂售出清 / 赠送亲友 / 留在库中)
                    val actionsRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, activity.dpToPx(8), 0, 0)
                    }

                    val btnDraft = TextView(activity).apply {
                        text = "✨ AI出清文案"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                        setBackgroundResource(R.drawable.bg_btn_custom_add)
                        setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                        applyPressScaleAnimation(0.92f)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = activity.dpToPx(6)
                        }
                        layoutParams = lp
                        setOnClickListener {
                            showListingCopilotDialog(activity, item) {
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    val btnSell = TextView(activity).apply {
                        text = "💰 挂售变现"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.accent_dark))
                        setBackgroundResource(R.drawable.bg_btn_custom_add)
                        setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                        applyPressScaleAnimation(0.92f)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = activity.dpToPx(6)
                        }
                        layoutParams = lp
                        setOnClickListener {
                            promptSellItem(activity, store, item) {
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    val btnGift = TextView(activity).apply {
                        text = "🎁 赠送"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setBackgroundResource(R.drawable.bg_chip_inactive)
                        setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                        applyPressScaleAnimation(0.92f)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = activity.dpToPx(6)
                        }
                        layoutParams = lp
                        setOnClickListener {
                            promptGiftItem(activity, store, item) {
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    val btnKeep = TextView(activity).apply {
                        text = "✅ 留在库中"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setBackgroundResource(R.drawable.bg_chip_inactive)
                        setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                        applyPressScaleAnimation(0.92f)
                        setOnClickListener {
                            val all = store.loadAll().toMutableList()
                            val idx = all.indexOfFirst { it.id == item.id }
                            if (idx != -1) {
                                all[idx] = item.copy(lastCheckedAt = System.currentTimeMillis())
                                store.saveAll(all)
                                Toast.makeText(activity, "已确认【${item.brand}】在位保留！", Toast.LENGTH_SHORT).show()
                                refreshContent()
                                onDataChanged?.invoke()
                            }
                        }
                    }

                    actionsRow.addView(btnDraft)
                    actionsRow.addView(btnSell)
                    actionsRow.addView(btnGift)
                    actionsRow.addView(btnKeep)
                    rowLayout.addView(actionsRow)

                    card.addView(rowLayout)
                    listLayout.addView(card)
                }

                scroll.addView(listLayout)
                dialogView.addView(scroll)
            }
        }

        refreshContent()
        dialog.show()
    }

    /** 弹出转卖回血登记弹窗 */
    private fun promptSellItem(activity: Activity, store: DataStore, item: Entry, onDone: () -> Unit) {
        val draft = generateListing(item)
        ModernDialogHelper.showInputDialog(
            context = activity,
            title = "转卖回血出清 · 挂闲鱼/转转",
            subtitle = "输入预计或实际转卖成交金额 (元)",
            hint = "参考秒出价 ¥${String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice)}",
            defaultValue = String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice),
            emoji = "💰"
        ) { text ->
            val sellVal = text.toDoubleOrNull() ?: draft.fastSellPrice
            val all = store.loadAll().toMutableList()
            val idx = all.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                all[idx] = item.copy(
                    isRetired = true,
                    retiredDate = System.currentTimeMillis(),
                    retiredAction = "📦 挂闲鱼代售",
                    currentValuation = sellVal
                )
                store.saveAll(all)
                Toast.makeText(activity, "🎉 已将【${item.brand}】归置出清！回血 ¥${String.format(Locale.getDefault(), "%.2f", sellVal)} 冲抵总支出", Toast.LENGTH_LONG).show()
                onDone()
            }
        }
    }

    /** 弹出赠送亲友登记弹窗 */
    private fun promptGiftItem(activity: Activity, store: DataStore, item: Entry, onDone: () -> Unit) {
        ModernDialogHelper.showInputDialog(
            context = activity,
            title = "赠送亲友出清",
            subtitle = "填写受赠亲友姓名或备注说明",
            hint = "如：送给表弟、赠送邻居",
            defaultValue = "赠送亲友",
            emoji = "🎁"
        ) { text ->
            val all = store.loadAll().toMutableList()
            val idx = all.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                val actionName = if (text.isNotBlank()) "🎁 $text" else "🎁 赠送亲友"
                all[idx] = item.copy(
                    isRetired = true,
                    retiredDate = System.currentTimeMillis(),
                    retiredAction = actionName
                )
                store.saveAll(all)
                Toast.makeText(activity, "🎉 已将【${item.brand}】标记为【$actionName】！", Toast.LENGTH_SHORT).show()
                onDone()
            }
        }
    }

    /** 弹出单件物品闲鱼智能文案与定价弹窗 */
    fun showListingCopilotDialog(activity: Activity, entry: Entry, onPriceSelected: ((Double) -> Unit)? = null) {
        val draft = generateListing(entry)

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(activity.dpToPx(16), activity.dpToPx(16), activity.dpToPx(16), activity.dpToPx(16))
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val titleTv = TextView(activity).apply {
            text = "✨ AI 闲鱼/转转出清文案助手"
            textSize = 16f
            paint.isFakeBoldText = true
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, activity.dpToPx(6))
        }

        val priceAdviceTv = TextView(activity).apply {
            text = "💰 行情估价：秒出价 ¥${String.format(Locale.getDefault(), "%.2f", draft.fastSellPrice)} | 建议挂牌价 ¥${String.format(Locale.getDefault(), "%.2f", draft.suggestPrice)}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            paint.isFakeBoldText = true
            setPadding(0, 0, 0, activity.dpToPx(10))
        }

        val editTitle = EditText(activity).apply {
            setText(draft.title)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setBackgroundResource(R.drawable.bg_input_box)
            setPadding(activity.dpToPx(10), activity.dpToPx(8), activity.dpToPx(10), activity.dpToPx(8))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = activity.dpToPx(8)
            }
            layoutParams = lp
        }

        val editDesc = EditText(activity).apply {
            setText(draft.description)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setBackgroundResource(R.drawable.bg_input_box)
            setLines(6)
            gravity = Gravity.TOP
            setPadding(activity.dpToPx(10), activity.dpToPx(8), activity.dpToPx(10), activity.dpToPx(8))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = activity.dpToPx(12)
            }
            layoutParams = lp
        }

        val btnCopy = TextView(activity).apply {
            text = "📋 复制完整商品文案至剪贴板"
            textSize = 13f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setBackgroundResource(R.drawable.bg_btn_primary)
            setPadding(0, activity.dpToPx(12), 0, activity.dpToPx(12))
            applyPressScaleAnimation(0.92f)
            setOnClickListener {
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val fullText = "${editTitle.text}\n\n${editDesc.text}"
                cm.setPrimaryClip(ClipData.newPlainText("Xianyu_Listing", fullText))
                Toast.makeText(activity, "🎉 已将标题与描述复制到剪贴板，可直接前往闲鱼/转转粘贴发布！", Toast.LENGTH_LONG).show()
                onPriceSelected?.invoke(draft.fastSellPrice)
                dialog.dismiss()
            }
        }

        dialogView.addView(titleTv)
        dialogView.addView(priceAdviceTv)
        dialogView.addView(editTitle)
        dialogView.addView(editDesc)
        dialogView.addView(btnCopy)

        dialog.show()
    }

    private fun Activity.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
