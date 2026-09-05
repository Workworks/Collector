package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 🔍 全局跨馆极速检索对话框 (Global Search Dialog)
 * 一键贯通资产主库及 12 个专业收纳馆（卡券/证照/药箱/食材/荣誉/衣橱/应急/工具/绿植/宠物/藏书/茶窖）
 */
object GlobalSearchDialog {

    data class SearchMatch(
        val emoji: String,
        val vaultName: String,
        val title: String,
        val subtitle: String,
        val detail: String,
        val reference: String? = null
    )

    fun show(activity: Activity, store: DataStore, onFilterMainList: ((String) -> Unit)? = null) {
        val input = EditText(activity).apply {
            hint = "物品、位置、备注、证件、食材……"
            setSingleLine(true)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("搜索全部内容")
            .setMessage("一次搜索资产主库和所有专业馆。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("搜索", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val query = input.text.toString().trim()
                if (query.isEmpty()) {
                    input.error = "请输入搜索内容"
                    return@setOnClickListener
                }
                val results = searchAll(store, query.lowercase())
                dialog.dismiss()
                if (results.isEmpty()) {
                    Toast.makeText(activity, "没有找到与「$query」相关的内容", Toast.LENGTH_LONG).show()
                } else {
                    store.recordSearchHit(results.first().title)
                    showResultsDialog(activity, query, results, onFilterMainList)
                }
            }
            input.requestFocus()
        }
        dialog.show()
    }

    private fun searchAll(store: DataStore, q: String): List<SearchMatch> {
        val matches = mutableListOf<SearchMatch>()

        // 1. 资产主库
        try {
            store.loadAll().forEach { e ->
                if (e.brand.lowercase().contains(q) ||
                    e.category.lowercase().contains(q) ||
                    e.location.lowercase().contains(q) ||
                    e.notes.lowercase().contains(q) ||
                    e.roomName.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = if (e.isSubscription) "🔄" else "📦",
                        vaultName = if (e.isSubscription) "订阅资产" else "资产主库",
                        title = e.brand,
                        subtitle = "${e.category} · ${e.qty}${e.unit} · ¥${e.price}",
                        detail = listOfNotNull(
                            if (e.location.isNotBlank()) "位置: ${e.location}" else null,
                            if (e.notes.isNotBlank()) "备注: ${e.notes}" else null
                        ).joinToString(" | "),
                        reference = "entries:${e.id}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索资产主库失败", e)
        }

        // 2. 🎟️ 时效卡券
        try {
            store.getVouchers().forEach { v ->
                if (v.title.lowercase().contains(q) ||
                    v.platform.lowercase().contains(q) ||
                    v.notes.lowercase().contains(q) ||
                    v.code.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🎟️",
                        vaultName = "时效卡券馆",
                        title = v.title,
                        subtitle = "${v.getTypeDisplayName()} · ${v.platform}",
                        detail = "面额: ${v.getDisplayValue()} | 备注: ${v.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索卡券失败", e)
        }

        // 3. 🪪 家庭证照
        try {
            store.getIdentityDocs().forEach { doc ->
                if (doc.nameOnDoc.lowercase().contains(q) ||
                    doc.member.lowercase().contains(q) ||
                    doc.docType.lowercase().contains(q) ||
                    doc.issuingAuthority.lowercase().contains(q) ||
                    doc.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🪪",
                        vaultName = "家庭证照馆",
                        title = "${doc.getDocTypeDisplayName()} (${doc.member})",
                        subtitle = "姓名: ${doc.nameOnDoc} · 机关: ${doc.issuingAuthority}",
                        detail = "号码: ${doc.getMaskedNumber()} | 备注: ${doc.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索证照失败", e)
        }

        // 4. 💊 家庭药箱
        try {
            store.getMedicines().forEach { m ->
                if (m.name.lowercase().contains(q) ||
                    m.category.lowercase().contains(q) ||
                    m.location.lowercase().contains(q) ||
                    m.dosage.lowercase().contains(q) ||
                    m.contraindications.lowercase().contains(q) ||
                    m.targetAudience.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "💊",
                        vaultName = "家庭药箱",
                        title = m.name,
                        subtitle = "${m.getCategoryDisplayName()} · ${m.qty}${m.unit} · ${m.location}",
                        detail = "用法: ${m.dosage} | 状态: ${m.getExpiryStatusText()}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索药箱失败", e)
        }

        // 5. 🥦 食材鲜度库
        try {
            store.getFoods().forEach { f ->
                if (f.name.lowercase().contains(q) ||
                    f.zone.lowercase().contains(q) ||
                    f.location.lowercase().contains(q) ||
                    f.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🥦",
                        vaultName = "食材鲜度库",
                        title = f.name,
                        subtitle = "${f.getZoneDisplayName()} · ${f.qty}${f.unit} · ${f.location}",
                        detail = "鲜度: ${f.getFreshnessStatusText()} | 备注: ${f.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索食材失败", e)
        }

        // 6. 🏆 荣誉考级
        try {
            store.getHonorCredentials().forEach { h ->
                if (h.title.lowercase().contains(q) ||
                    h.member.lowercase().contains(q) ||
                    h.issuer.lowercase().contains(q) ||
                    h.certNumber.lowercase().contains(q) ||
                    h.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🏆",
                        vaultName = "荣誉勋章馆",
                        title = h.title,
                        subtitle = "${h.getCategoryDisplayName()} · 归属: ${h.member}",
                        detail = "发证: ${h.issuer} | 证书号: ${h.getMaskedCertNumber()}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索荣誉失败", e)
        }

        // 7. 👗 换季衣橱
        try {
            store.getWardrobeRecords().forEach { w ->
                if (w.name.lowercase().contains(q) ||
                    w.color.lowercase().contains(q) ||
                    w.material.lowercase().contains(q) ||
                    w.storageLocation.lowercase().contains(q) ||
                    w.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "👗",
                        vaultName = "四季衣橱馆",
                        title = w.name,
                        subtitle = "${w.getSeasonDisplayName()} · ${w.color} · ${w.storageLocation}",
                        detail = "面料: ${w.material} | 穿搭: ${w.wearCount}次"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索衣橱失败", e)
        }

        // 8. 🚨 应急防灾
        try {
            store.getEmergencyItems().forEach { em ->
                if (em.name.lowercase().contains(q) ||
                    em.kitType.lowercase().contains(q) ||
                    em.location.lowercase().contains(q) ||
                    em.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🚨",
                        vaultName = "应急防灾馆",
                        title = em.name,
                        subtitle = "${em.getKitTypeDisplayName()} · ${em.qty}${em.unit} · ${em.location}",
                        detail = "类别: ${em.getCategoryDisplayName()} | 备忘: ${em.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索应急物资失败", e)
        }

        // 9. 🔧 工具维保
        try {
            store.getToolRecords().forEach { t ->
                if (t.name.lowercase().contains(q) ||
                    t.spec.lowercase().contains(q) ||
                    t.category.lowercase().contains(q) ||
                    t.location.lowercase().contains(q) ||
                    t.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🔧",
                        vaultName = "工具维保馆",
                        title = t.name,
                        subtitle = "${t.getCategoryDisplayName()} · ${t.spec} · ${t.location}",
                        detail = "数量: ${t.qty}${t.unit} | 备忘: ${t.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索工具失败", e)
        }

        // 10. 🪴 绿植水肥
        try {
            store.getPlantRecords().forEach { p ->
                if (p.name.lowercase().contains(q) ||
                    p.species.lowercase().contains(q) ||
                    p.location.lowercase().contains(q) ||
                    p.careTips.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🪴",
                        vaultName = "绿植养护馆",
                        title = p.name,
                        subtitle = "${p.species} · ${p.getLightDemandDisplayName()} · ${p.location}",
                        detail = "浇水周期: 每${p.waterIntervalDays}天 | 要领: ${p.careTips}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索绿植失败", e)
        }

        // 11. 🐾 萌宠健康
        try {
            store.getPetRecords().forEach { pet ->
                if (pet.name.lowercase().contains(q) ||
                    pet.species.lowercase().contains(q) ||
                    pet.foodBrand.lowercase().contains(q) ||
                    pet.notes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🐾",
                        vaultName = "萌宠档案馆",
                        title = pet.name,
                        subtitle = "${pet.species} · 体重: ${pet.weightKg}kg · 主粮: ${pet.foodBrand}",
                        detail = "驱虫周期: 每${pet.dewormIntervalDays}天 | 备忘: ${pet.notes}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索萌宠失败", e)
        }

        // 12. 📚 书房藏书
        try {
            store.getBookRecords().forEach { b ->
                if (b.title.lowercase().contains(q) ||
                    b.author.lowercase().contains(q) ||
                    b.bookshelfLocation.lowercase().contains(q) ||
                    b.borrowerName.lowercase().contains(q) ||
                    b.summaryNotes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "📚",
                        vaultName = "书房藏书馆",
                        title = b.title,
                        subtitle = "作者: ${b.author} · ${b.getStatusDisplayName()} · ${b.bookshelfLocation}",
                        detail = "进度: ${b.getProgressPercent()}% (${b.currentPages}/${b.totalPages}页) | 摘录: ${b.summaryNotes.take(40)}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索藏书失败", e)
        }

        // 13. 🍷 茶窖名酿
        try {
            store.getBeverageRecords().forEach { bv ->
                if (bv.name.lowercase().contains(q) ||
                    bv.category.lowercase().contains(q) ||
                    bv.originRegion.lowercase().contains(q) ||
                    bv.storageLocation.lowercase().contains(q) ||
                    bv.tastingNotes.lowercase().contains(q)
                ) {
                    matches.add(SearchMatch(
                        emoji = "🍷",
                        vaultName = "茶窖名酿馆",
                        title = bv.name,
                        subtitle = "${bv.category} · ${bv.getStatusDisplayName()} · ${bv.storageLocation}",
                        detail = "产区: ${bv.originRegion} | 数量: ${bv.qty}${bv.unit} | 风味: ${bv.tastingNotes.take(40)}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索茶窖失败", e)
        }

        // 14. 💡 闪念灵感与想法
        try {
            store.getIdeas().forEach { idea ->
                if (idea.content.lowercase().contains(q) ||
                    idea.tags.any { it.lowercase().contains(q) }
                ) {
                    matches.add(SearchMatch(
                        emoji = idea.moodEmoji,
                        vaultName = "灵感想法舱",
                        title = idea.getPreview(25),
                        subtitle = if (idea.tags.isNotEmpty()) "标签: ${idea.tags.joinToString(", ")}" else "闪念想法",
                        detail = "正文: ${idea.content.replace("\n", " ").take(45)}"
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索灵感想法失败", e)
        }

        // 15. 📰 智能剪藏与知识库
        try {
            store.getClippings().forEach { clip ->
                if (clip.getSearchableContent().lowercase().contains(q)) {
                    val preview = if (clip.ocrRawText.isNotBlank()) "OCR: " + clip.ocrRawText.replace("\n", " ").take(40) else clip.summary.take(40)
                    matches.add(SearchMatch(
                        emoji = if (clip.sourcePlatform == "screenshot") "📸" else "📰",
                        vaultName = "剪藏知识库",
                        title = if (clip.title.isNotBlank()) clip.title else "剪藏内容",
                        subtitle = "${clip.sourcePlatform} · 标签: ${clip.tags.joinToString(", ")}",
                        detail = preview
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GlobalSearch", "检索剪藏知识失败", e)
        }

        return matches
    }

    private fun showResultsDialog(
        activity: Activity,
        query: String,
        results: List<SearchMatch>,
        onFilterMainList: ((String) -> Unit)?
    ) {
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        // 头部标题
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
            layoutParams = lp
        }

        val emojiTv = TextView(activity).apply {
            text = "🔍"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        header.addView(emojiTv)

        val titleTv = TextView(activity).apply {
            text = "全库找到 ${results.size} 条与「$query」相关的记录"
            textSize = 15f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            layoutParams = lp
        }
        header.addView(titleTv)
        root.addView(header)

        // 滚动列表
        val scroll = ScrollView(activity).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(minOf(340, maxOf(92, results.size * 92)))
            )
            layoutParams = lp
            isVerticalScrollBarEnabled = true
        }

        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for (match in results) {
            val card = MaterialCardView(activity).apply {
                radius = dp(12).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColor(activity, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                layoutParams = lp
            }

            val cardLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tagTv = TextView(activity).apply {
                text = "${match.emoji} ${match.vaultName}"
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(tagTv)
            cardLayout.addView(topRow)

            val titleView = TextView(activity).apply {
                text = match.title
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
                layoutParams = lp
            }
            cardLayout.addView(titleView)

            if (match.subtitle.isNotBlank()) {
                val subView = TextView(activity).apply {
                    text = match.subtitle
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(2) }
                    layoutParams = lp
                }
                cardLayout.addView(subView)
            }

            if (match.detail.isNotBlank()) {
                val detailView = TextView(activity).apply {
                    text = match.detail
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(2) }
                    layoutParams = lp
                }
                cardLayout.addView(detailView)
            }

            card.addView(cardLayout)
            match.reference?.let { reference ->
                card.isClickable = true
                card.isFocusable = true
                card.contentDescription = "打开${match.title}找回卡"
                card.applyPressScaleAnimation(0.97f)
                card.setOnClickListener { SearchResultDetailDialog.show(activity, reference) }
                tagTv.text = "${match.emoji} ${match.vaultName}  ·  点击找回"
            }
            listContainer.addView(card)
        }

        scroll.addView(listContainer)
        root.addView(scroll)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 底部按键组
        val buttonBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            layoutParams = lp
        }

        val btnFilterMain = Button(activity).apply {
            text = "仅筛选主库"
            textSize = 13f
            val lp = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) }
            layoutParams = lp
            applyPressScaleAnimation(0.92f)
            setOnClickListener {
                dialog.dismiss()
                onFilterMainList?.invoke(query)
            }
        }
        buttonBar.addView(btnFilterMain)

        val btnClose = Button(activity).apply {
            text = "关闭"
            textSize = 13f
            val lp = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(6) }
            layoutParams = lp
            applyPressScaleAnimation(0.92f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonBar.addView(btnClose)

        root.addView(buttonBar)

        dialog.show()
    }
}
