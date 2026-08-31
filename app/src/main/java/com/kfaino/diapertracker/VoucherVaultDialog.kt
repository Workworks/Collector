package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogAddVoucherBinding
import com.kfaino.diapertracker.databinding.DialogVoucherVaultBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎟️ 时效权益与卡券票据收纳馆控制器 (Voucher Vault Dialog Controller)
 * - 统一收纳优惠券、代金券、洗车/健身次卡、会员每月专属权益
 * - 临期强提醒、次卡一键「-1次」核销扣减、核销码一键复制
 */
object VoucherVaultDialog {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 打开时效卡券收纳馆主弹窗 */
    fun showVoucherVaultDialog(
        activity: Activity,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogVoucherVaultBinding.inflate(activity.layoutInflater)
        var currentFilter = "active" // "active", "expiring", "times_card", "history"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        VaultUiHelper.setupVaultWindow(dialog)

        fun refreshList() {
            val all = store.getVouchers()
            val activeList = all.filter { !it.isUsed && !it.isExpired() }
            val expiringList = activeList.filter { it.isExpiringSoon() }
            val timesCardList = all.filter { it.type == "times_card" && !it.isUsed }
            val historyList = all.filter { it.isUsed || it.isExpired() }

            // 统计看板
            binding.tvVoucherActiveCount.text = "${activeList.size} 张"
            val totalValue = activeList.filter { it.type != "times_card" }.sumOf { it.valueAmount }
            binding.tvVoucherTotalValue.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalValue)}"
            binding.tvVoucherExpiringCount.text = "${expiringList.size} 项"

            val displayList = when (currentFilter) {
                "expiring" -> expiringList
                "times_card" -> timesCardList
                "history" -> historyList
                else -> activeList
            }

            renderVoucherList(activity, store, displayList, currentFilter, binding.voucherListContainer) {
                refreshList()
                onUpdated()
            }
        }

        fun updateTabs() {
            val activeBg = R.drawable.bg_chip_active
            val inActiveBg = R.drawable.bg_chip_inactive
            val white = Color.WHITE
            val secColor = ContextCompat.getColor(activity, R.color.text_secondary)

            binding.tabVoucherActive.setBackgroundResource(if (currentFilter == "active") activeBg else inActiveBg)
            binding.tabVoucherActive.setTextColor(if (currentFilter == "active") white else secColor)

            binding.tabVoucherExpiring.setBackgroundResource(if (currentFilter == "expiring") activeBg else inActiveBg)
            binding.tabVoucherExpiring.setTextColor(if (currentFilter == "expiring") white else secColor)

            binding.tabVoucherTimesCard.setBackgroundResource(if (currentFilter == "times_card") activeBg else inActiveBg)
            binding.tabVoucherTimesCard.setTextColor(if (currentFilter == "times_card") white else secColor)

            binding.tabVoucherHistory.setBackgroundResource(if (currentFilter == "history") activeBg else inActiveBg)
            binding.tabVoucherHistory.setTextColor(if (currentFilter == "history") white else secColor)
        }

        binding.tabVoucherActive.setOnClickListener { currentFilter = "active"; updateTabs(); refreshList() }
        binding.tabVoucherExpiring.setOnClickListener { currentFilter = "expiring"; updateTabs(); refreshList() }
        binding.tabVoucherTimesCard.setOnClickListener { currentFilter = "times_card"; updateTabs(); refreshList() }
        binding.tabVoucherHistory.setOnClickListener { currentFilter = "history"; updateTabs(); refreshList() }

        binding.btnCloseVoucherVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseVoucherVault.setOnClickListener { dialog.dismiss() }

        binding.btnOpenAddVoucher.applyPressScaleAnimation(0.92f)
        binding.btnOpenAddVoucher.setOnClickListener {
            showAddOrEditVoucherDialog(activity, store, voucher = null) {
                refreshList()
                onUpdated()
            }
        }

        refreshList()
        dialog.show()
    }

    /** 动态渲染卡券列表卡片 */
    private fun renderVoucherList(
        activity: Activity,
        store: DataStore,
        list: List<VoucherRecord>,
        currentFilter: String,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (list.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = if (currentFilter == "history") "📜 暂无已核销或失效的历史卡券" else if (currentFilter == "expiring") "🎉 太棒了！当前没有 3 天内即将过期的卡券" else "🎟️ 暂无在手可用的优惠券或次卡\n点击下方按钮立即登记您的第一张卡券吧~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyTv)
            return
        }

        for (v in list) {
            val card = MaterialCardView(activity).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColor(activity, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                layoutParams = lp
            }

            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            // 1. 顶行：面额/余量 + 券名 + 临期状态标签
            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val valTv = TextView(activity).apply {
                text = v.getDisplayValue()
                textSize = 16f
                paint.isFakeBoldText = true
                setTextColor(if (v.isUsed || v.isExpired()) ContextCompat.getColor(activity, R.color.text_tertiary) else ContextCompat.getColor(activity, R.color.primary))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(8)
                }
            }

            val nameTv = TextView(activity).apply {
                text = v.title
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val statusBadge = TextView(activity).apply {
                val isExpiring = v.isExpiringSoon()
                val isExpired = v.isExpired()
                val isUsed = v.isUsed

                val textStr = if (isUsed) "⚪ 已核销" else if (isExpired) "🔴 已过期" else if (isExpiring) "⏰ 临期抢用" else v.getTypeDisplayName().take(4)
                val textColor = if (isExpiring) ContextCompat.getColor(activity, R.color.accent_dark) else if (isExpired) ContextCompat.getColor(activity, R.color.danger) else ContextCompat.getColor(activity, R.color.text_secondary)
                text = textStr
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(textColor)
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(6), dp(2), dp(6), dp(2))
            }

            topRow.addView(valTv)
            topRow.addView(nameTv)
            topRow.addView(statusBadge)
            root.addView(topRow)

            // 2. 次级详情 (门槛 + 到期日 + 平台)
            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }

            val minSpendStr = if (v.minSpend > 0) "满 ¥${String.format(Locale.getDefault(), "%.0f", v.minSpend)} 可用" else "无门槛"
            val platStr = if (v.platform.isNotBlank()) " · 适用: ${v.platform}" else ""
            val expStr = if (v.expiryDate > 0L) "📅 截止: ${dateFormat.format(Date(v.expiryDate))}" else "📅 长期有效"

            val descTv = TextView(activity).apply {
                text = "$minSpendStr$platStr   $expStr"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            }
            infoLayout.addView(descTv)

            if (v.code.isNotBlank()) {
                val codeTv = TextView(activity).apply {
                    text = "🔑 券码: ${v.code} (点击复制)"
                    textSize = 12f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(activity, R.color.accent_dark))
                    setOnClickListener {
                        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Voucher Code", v.code))
                        Toast.makeText(activity, "已复制券码: ${v.code}", Toast.LENGTH_SHORT).show()
                    }
                }
                infoLayout.addView(codeTv)
            }

            if (v.notes.isNotBlank()) {
                val notesTv = TextView(activity).apply {
                    text = "📝 规则: ${v.notes}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    maxLines = 2
                }
                infoLayout.addView(notesTv)
            }

            root.addView(infoLayout)

            // 3. 底部快捷操作栏
            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(4), 0, 0)
            }

            if (!v.isUsed && !v.isExpired()) {
                if (v.type == "times_card") {
                    val btnUseOnce = TextView(activity).apply {
                        text = "🎫 消费-1次"
                        textSize = 12f
                        paint.isFakeBoldText = true
                        setTextColor(ContextCompat.getColor(activity, R.color.primary))
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        setOnClickListener {
                            store.useTimesCardOneTime(v.id)
                            Toast.makeText(activity, "🎉 次卡已打卡扣减 1 次！", Toast.LENGTH_SHORT).show()
                            onRefreshNeeded()
                        }
                    }
                    btnRow.addView(btnUseOnce)
                }

                val btnMarkUsed = TextView(activity).apply {
                    text = "✅ 标记已核销"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        store.markVoucherUsed(v.id, true)
                        Toast.makeText(activity, "已标记为已核销并归档", Toast.LENGTH_SHORT).show()
                        onRefreshNeeded()
                    }
                }
                btnRow.addView(btnMarkUsed)
            }

            val btnEdit = TextView(activity).apply {
                text = "✏️ 编辑"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    showAddOrEditVoucherDialog(activity, store, v) {
                        onRefreshNeeded()
                    }
                }
            }

            val btnDelete = TextView(activity).apply {
                text = "🗑️ 删除"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.danger))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "确认删除卡券？",
                        message = "确定要删除【${v.title}】吗？此操作无法撤销。",
                        emoji = "🗑️",
                        positiveText = "确认删除",
                        negativeText = "取消"
                    ) {
                        store.deleteVoucher(v.id)
                        onRefreshNeeded()
                    }
                }
            }

            btnRow.addView(btnEdit)
            btnRow.addView(btnDelete)
            root.addView(btnRow)

            card.addView(root)
            container.addView(card)
        }
    }

    /** 登记 / 编辑卡券权益弹窗 */
    fun showAddOrEditVoucherDialog(
        activity: Activity,
        store: DataStore,
        voucher: VoucherRecord?,
        onSaved: () -> Unit
    ) {
        val binding = DialogAddVoucherBinding.inflate(activity.layoutInflater)
        var selectedExpiryDate = voucher?.expiryDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        if (voucher != null) {
            binding.tvAddVoucherDialogTitle.text = "✏️ 编辑卡券权益"
            binding.etVoucherTitle.setText(voucher.title)
            binding.etVoucherValue.setText(if (voucher.valueAmount > 0) voucher.valueAmount.toString() else "")
            binding.etVoucherMinSpend.setText(if (voucher.minSpend > 0) voucher.minSpend.toString() else "")
            binding.etVoucherTotalTimes.setText(voucher.totalTimes.toString())
            binding.etVoucherRemainingTimes.setText(voucher.remainingTimes.toString())
            binding.etVoucherCode.setText(voucher.code)
            binding.etVoucherPlatform.setText(voucher.platform)
            binding.etVoucherNotes.setText(voucher.notes)

            when (voucher.type) {
                "times_card" -> binding.rbTypeTimesCard.isChecked = true
                "cash_voucher" -> binding.rbTypeCash.isChecked = true
                "privilege" -> binding.rbTypePrivilege.isChecked = true
                else -> binding.rbTypeCoupon.isChecked = true
            }
        }

        fun updateTypeVisibility() {
            val isTimesCard = binding.rbTypeTimesCard.isChecked
            binding.layoutTimesCardInputs.visibility = if (isTimesCard) View.VISIBLE else View.GONE
            binding.layoutAmountInputs.visibility = if (isTimesCard) View.GONE else View.VISIBLE
        }
        updateTypeVisibility()

        binding.rgVoucherType.setOnCheckedChangeListener { _, _ ->
            updateTypeVisibility()
        }

        fun updateExpiryText() {
            if (selectedExpiryDate > 0L) {
                binding.tvVoucherExpiryPicker.text = "📅 截止日期: " + dateFormat.format(Date(selectedExpiryDate))
            } else {
                binding.tvVoucherExpiryPicker.text = "📅 长期有效 (点击设定截止日期)"
            }
        }
        updateExpiryText()

        binding.tvVoucherExpiryPicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, if (selectedExpiryDate > 0) selectedExpiryDate else System.currentTimeMillis(), "选择卡券截止日期") { timeMs ->
                selectedExpiryDate = timeMs
                updateExpiryText()
            }
        }

        binding.btnCloseAddVoucher.applyPressScaleAnimation(0.92f)
        binding.btnCloseAddVoucher.setOnClickListener { dialog.dismiss() }

        binding.btnCancelVoucher.applyPressScaleAnimation(0.92f)
        binding.btnCancelVoucher.setOnClickListener { dialog.dismiss() }

        binding.btnConfirmVoucher.applyPressScaleAnimation(0.92f)
        binding.btnConfirmVoucher.setOnClickListener {
            val title = binding.etVoucherTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(activity, "请输入卡券或权益名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = when {
                binding.rbTypeTimesCard.isChecked -> "times_card"
                binding.rbTypeCash.isChecked -> "cash_voucher"
                binding.rbTypePrivilege.isChecked -> "privilege"
                else -> "coupon"
            }

            val valAmount = binding.etVoucherValue.text.toString().toDoubleOrNull() ?: 0.0
            val minSpend = binding.etVoucherMinSpend.text.toString().toDoubleOrNull() ?: 0.0
            val totTimes = binding.etVoucherTotalTimes.text.toString().toIntOrNull() ?: 1
            val remTimes = binding.etVoucherRemainingTimes.text.toString().toIntOrNull() ?: totTimes
            val code = binding.etVoucherCode.text.toString().trim()
            val platform = binding.etVoucherPlatform.text.toString().trim()
            val notes = binding.etVoucherNotes.text.toString().trim()

            val record = VoucherRecord(
                id = voucher?.id ?: UUID.randomUUID().toString(),
                title = title,
                type = type,
                valueAmount = valAmount,
                minSpend = minSpend,
                remainingTimes = remTimes,
                totalTimes = totTimes,
                startDate = voucher?.startDate ?: System.currentTimeMillis(),
                expiryDate = selectedExpiryDate,
                code = code,
                platform = platform,
                photoPath = voucher?.photoPath ?: "",
                notes = notes,
                isUsed = voucher?.isUsed ?: false,
                usedAt = voucher?.usedAt ?: 0L
            )

            store.addOrUpdateVoucher(record)
            Toast.makeText(activity, "🎉 卡券权益【$title】已成功保存！", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
