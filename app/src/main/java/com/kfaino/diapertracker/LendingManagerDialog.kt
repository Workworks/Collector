package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogLendOutBinding
import com.kfaino.diapertracker.databinding.DialogLendingManagerBinding
import com.kfaino.diapertracker.databinding.DialogReturnItemBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📤 实物外借与共享借还管理中心控制器 (Lending Manager Dialog Controller)
 * - 集中管控借出中物品、逾期风控预警与历史借还档案
 * - 支持借出登记、电子借条/催还凭据海报生成、归还打卡
 */
object LendingManagerDialog {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 打开实物外借中心主弹窗 */
    fun showLendingHubDialog(
        activity: Activity,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogLendingManagerBinding.inflate(activity.layoutInflater)
        var currentFilter = "active" // "active", "overdue", "history"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        fun refreshList() {
            val allEntries = store.loadAll()
            val activeLent = allEntries.filter { it.isLentOut }
            val overdueLent = activeLent.filter { it.isLendingOverdue() }
            val historyEntries = allEntries.filter { it.lendingHistory.isNotEmpty() && !it.isLentOut }

            // 统计看板
            binding.tvLendingActiveCount.text = "${activeLent.size} 件"
            val totalWorth = activeLent.sumOf { it.price * it.qty }
            binding.tvLendingTotalWorth.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalWorth)}"
            binding.tvLendingOverdueCount.text = "${overdueLent.size} 项"

            val displayList = when (currentFilter) {
                "overdue" -> overdueLent
                "history" -> historyEntries
                else -> activeLent
            }

            renderLendingList(activity, store, displayList, currentFilter, binding.lendingListContainer) {
                refreshList()
                onUpdated()
            }
        }

        fun updateTabs() {
            val activeBg = R.drawable.bg_chip_active
            val inActiveBg = R.drawable.bg_chip_inactive
            val white = Color.WHITE
            val secColor = ContextCompat.getColor(activity, R.color.text_secondary)

            binding.tabLendingActive.setBackgroundResource(if (currentFilter == "active") activeBg else inActiveBg)
            binding.tabLendingActive.setTextColor(if (currentFilter == "active") white else secColor)

            binding.tabLendingOverdue.setBackgroundResource(if (currentFilter == "overdue") activeBg else inActiveBg)
            binding.tabLendingOverdue.setTextColor(if (currentFilter == "overdue") white else secColor)

            binding.tabLendingHistory.setBackgroundResource(if (currentFilter == "history") activeBg else inActiveBg)
            binding.tabLendingHistory.setTextColor(if (currentFilter == "history") white else secColor)
        }

        binding.tabLendingActive.setOnClickListener { currentFilter = "active"; updateTabs(); refreshList() }
        binding.tabLendingOverdue.setOnClickListener { currentFilter = "overdue"; updateTabs(); refreshList() }
        binding.tabLendingHistory.setOnClickListener { currentFilter = "history"; updateTabs(); refreshList() }

        binding.btnCloseLendingHub.applyPressScaleAnimation(0.92f)
        binding.btnCloseLendingHub.setOnClickListener { dialog.dismiss() }

        binding.btnOpenLendOutPicker.applyPressScaleAnimation(0.92f)
        binding.btnOpenLendOutPicker.setOnClickListener {
            showLendOutDialog(activity, store, targetEntry = null) {
                refreshList()
                onUpdated()
            }
        }

        refreshList()
        dialog.show()
    }

    /** 动态渲染借出与流转卡片列表 */
    private fun renderLendingList(
        activity: Activity,
        store: DataStore,
        list: List<Entry>,
        currentFilter: String,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (list.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = if (currentFilter == "history") "📜 暂无历史归还档案记录" else if (currentFilter == "overdue") "🎉 暂无逾期外借物品，借还诚信极佳！" else "📤 当前暂无外借物品\n点击下方按钮即可为朋友/同事登记物品外借~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyTv)
            return
        }

        for (entry in list) {
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

            // 1. 顶行：物品名 + 借还状态徽章
            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val nameTv = TextView(activity).apply {
                text = entry.brand
                textSize = 15f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val statusBadge = TextView(activity).apply {
                val isOverdue = entry.isLendingOverdue()
                val textStr = if (!entry.isLentOut) "⚪ 已归还入库" else if (isOverdue) "🔴 逾期未还" else "🟢 外借中"
                val textColor = if (isOverdue) ContextCompat.getColor(activity, R.color.danger) else if (entry.isLentOut) ContextCompat.getColor(activity, R.color.primary) else ContextCompat.getColor(activity, R.color.text_secondary)
                text = textStr
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(textColor)
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }

            topRow.addView(nameTv)
            topRow.addView(statusBadge)
            root.addView(topRow)

            // 2. 次级借还信息
            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(4), 0, dp(6))
            }

            val record = entry.getCurrentLendingRecord()
            val borrowerText = if (entry.isLentOut) "👤 借用人: ${entry.currentBorrower}" else if (record != null) "👤 曾借给: ${record.borrowerName}" else "👤 借用记录"
            val contactText = if (entry.currentBorrowerContact.isNotBlank()) " (${entry.currentBorrowerContact})" else ""

            val borrowerTv = TextView(activity).apply {
                text = borrowerText + contactText
                textSize = 12f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            }
            infoLayout.addView(borrowerTv)

            val statusDescTv = TextView(activity).apply {
                text = entry.getLendingStatusText()
                textSize = 12f
                setTextColor(if (entry.isLendingOverdue()) ContextCompat.getColor(activity, R.color.danger) else ContextCompat.getColor(activity, R.color.text_hint))
            }
            infoLayout.addView(statusDescTv)

            if (record != null && record.notes.isNotBlank()) {
                val notesTv = TextView(activity).apply {
                    text = "📦 附带配件: ${record.notes}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    maxLines = 2
                }
                infoLayout.addView(notesTv)
            }

            root.addView(infoLayout)

            // 3. 底部操作栏
            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(4), 0, 0)
            }

            if (entry.isLentOut) {
                // 催还提醒海报 / 复制微信文案
                val btnRemind = TextView(activity).apply {
                    text = "💬 催还海报"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.accent_dark))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        val activeRecord = entry.getCurrentLendingRecord() ?: LendingRecord(borrowerName = entry.currentBorrower)
                        exportAndShareVoucher(activity, entry, activeRecord, isReminderMode = true)
                    }
                }

                // 电子借条凭证
                val btnVoucher = TextView(activity).apply {
                    text = "📜 电子借条"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        val activeRecord = entry.getCurrentLendingRecord() ?: LendingRecord(borrowerName = entry.currentBorrower)
                        exportAndShareVoucher(activity, entry, activeRecord, isReminderMode = false)
                    }
                }

                // 确认归还打卡
                val btnReturn = TextView(activity).apply {
                    text = "✅ 归还打卡"
                    textSize = 12f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(activity, R.color.primary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        showReturnDialog(activity, store, entry) {
                            onRefreshNeeded()
                        }
                    }
                }

                btnRow.addView(btnRemind)
                btnRow.addView(btnVoucher)
                btnRow.addView(btnReturn)
            } else {
                // 再次借出
                val btnReLend = TextView(activity).apply {
                    text = "📤 再次借出"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.primary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        showLendOutDialog(activity, store, targetEntry = entry) {
                            onRefreshNeeded()
                        }
                    }
                }
                btnRow.addView(btnReLend)
            }

            root.addView(btnRow)
            card.addView(root)
            container.addView(card)
        }
    }

    /** 登记物品外借弹窗 */
    fun showLendOutDialog(
        activity: Activity,
        store: DataStore,
        targetEntry: Entry?,
        onSaved: () -> Unit
    ) {
        val binding = DialogLendOutBinding.inflate(activity.layoutInflater)
        var selectedEntry: Entry? = targetEntry
        var selectedLentDate = System.currentTimeMillis()
        var selectedReturnDate = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 若未指定物品，提供选择器
        if (selectedEntry == null) {
            val available = store.loadAll().filter { !it.isLentOut && !it.isRetired }
            if (available.isEmpty()) {
                Toast.makeText(activity, "当前没有可借出的在库物品", Toast.LENGTH_SHORT).show()
                return
            }
            binding.tvLendOutItemName.text = "点击选择外借物品: 【${available.first().brand}】"
            selectedEntry = available.first()

            binding.tvLendOutItemName.setOnClickListener {
                val names = available.map { "【${it.category}】${it.brand}" }
                ModernDialogHelper.showSingleChoiceDialog(
                    context = activity,
                    title = "选择外借物品",
                    emoji = "📦",
                    options = names,
                    selectedIndex = available.indexOf(selectedEntry)
                ) { which, _ ->
                    selectedEntry = available[which]
                    binding.tvLendOutItemName.text = "当前外借物品: 【${selectedEntry!!.brand}】"
                }
            }
        } else {
            binding.tvLendOutItemName.text = "当前外借物品: 【${selectedEntry?.brand ?: ""}】"
        }

        binding.tvLentDatePicker.text = "📅 " + dateFormat.format(Date(selectedLentDate)) + " (今日)"
        binding.tvLentDatePicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, selectedLentDate, "选择借出交接日期") { timeMs ->
                selectedLentDate = timeMs
                binding.tvLentDatePicker.text = "📅 " + dateFormat.format(Date(selectedLentDate))
            }
        }

        fun updateCustomReturnText() {
            binding.tvCustomReturnDate.text = "📅 约定归还日期: " + dateFormat.format(Date(selectedReturnDate))
        }
        updateCustomReturnText()

        binding.rgReturnPeriod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbPeriod7d.id -> {
                    selectedReturnDate = selectedLentDate + 7L * 24 * 60 * 60 * 1000
                    binding.tvCustomReturnDate.visibility = View.VISIBLE
                    updateCustomReturnText()
                }
                binding.rbPeriod15d.id -> {
                    selectedReturnDate = selectedLentDate + 15L * 24 * 60 * 60 * 1000
                    binding.tvCustomReturnDate.visibility = View.VISIBLE
                    updateCustomReturnText()
                }
                binding.rbPeriod1m.id -> {
                    selectedReturnDate = selectedLentDate + 30L * 24 * 60 * 60 * 1000
                    binding.tvCustomReturnDate.visibility = View.VISIBLE
                    updateCustomReturnText()
                }
                binding.rbPeriodCustom.id -> {
                    binding.tvCustomReturnDate.visibility = View.VISIBLE
                    ModernDatePickerDialog.show(activity, selectedReturnDate, "选择预计归还日期") { timeMs ->
                        selectedReturnDate = timeMs
                        updateCustomReturnText()
                    }
                }
            }
        }

        binding.tvCustomReturnDate.setOnClickListener {
            ModernDatePickerDialog.show(activity, selectedReturnDate, "选择预计归还日期") { timeMs ->
                selectedReturnDate = timeMs
                binding.rbPeriodCustom.isChecked = true
                updateCustomReturnText()
            }
        }

        binding.btnCloseLendOut.applyPressScaleAnimation(0.92f)
        binding.btnCloseLendOut.setOnClickListener { dialog.dismiss() }

        binding.btnCancelLendOut.applyPressScaleAnimation(0.92f)
        binding.btnCancelLendOut.setOnClickListener { dialog.dismiss() }

        binding.btnConfirmLendOut.applyPressScaleAnimation(0.92f)
        binding.btnConfirmLendOut.setOnClickListener {
            val borrower = binding.etBorrowerName.text.toString().trim()
            if (borrower.isBlank()) {
                Toast.makeText(activity, "请输入借用人姓名或称呼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contact = binding.etBorrowerContact.text.toString().trim()
            val deposit = binding.etLendingDeposit.text.toString().toDoubleOrNull() ?: 0.0
            val notes = binding.etLendingNotes.text.toString().trim()

            val target = selectedEntry ?: return@setOnClickListener
            store.lendAsset(
                entryId = target.id,
                borrowerName = borrower,
                borrowerContact = contact,
                expectedReturnDate = selectedReturnDate,
                deposit = deposit,
                notes = notes
            )

            Toast.makeText(activity, "🎉 已成功为【${target.brand}】登记外借至【$borrower】！", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onSaved()

            // 提示是否生成电子借条
            val updated = store.loadAll().firstOrNull { it.id == target.id }
            val record = updated?.getCurrentLendingRecord()
            if (updated != null && record != null) {
                ModernDialogHelper.showConfirmDialog(
                    context = activity,
                    title = "生成电子借条凭证？",
                    message = "已登记成功！是否立即生成 1080P 高清电子借条凭证卡片，以便通过微信发送给【$borrower】？",
                    emoji = "📜",
                    positiveText = "立即生成",
                    negativeText = "稍后再说"
                ) {
                    exportAndShareVoucher(activity, updated, record, isReminderMode = false)
                }
            }
        }

        dialog.show()
    }

    /** 确认归还打卡弹窗 */
    fun showReturnDialog(
        activity: Activity,
        store: DataStore,
        entry: Entry,
        onReturned: () -> Unit
    ) {
        val binding = DialogReturnItemBinding.inflate(activity.layoutInflater)
        var selectedActualDate = System.currentTimeMillis()

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvReturnItemSub.text = "${entry.brand} · 借用人: ${entry.currentBorrower}"
        if (entry.currentDeposit > 0) {
            binding.tvReturnDepositHint.text = "💰 押金清退提醒：登记有押金 ￥${String.format(Locale.getDefault(), "%.2f", entry.currentDeposit)}，归还交接时请确认已结清。"
        } else {
            binding.tvReturnDepositHint.text = "💰 免押金外借，配件核对无误即可直接打卡入库。"
        }

        binding.tvActualReturnDatePicker.text = "📅 " + dateFormat.format(Date(selectedActualDate)) + " (今日)"
        binding.tvActualReturnDatePicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, selectedActualDate, "选择实际归还日期") { timeMs ->
                selectedActualDate = timeMs
                binding.tvActualReturnDatePicker.text = "📅 " + dateFormat.format(Date(selectedActualDate))
            }
        }

        binding.btnCloseReturnDialog.applyPressScaleAnimation(0.92f)
        binding.btnCloseReturnDialog.setOnClickListener { dialog.dismiss() }

        binding.btnCancelReturn.applyPressScaleAnimation(0.92f)
        binding.btnCancelReturn.setOnClickListener { dialog.dismiss() }

        binding.btnConfirmReturn.applyPressScaleAnimation(0.92f)
        binding.btnConfirmReturn.setOnClickListener {
            val rating = when {
                binding.rbRetStar1.isChecked -> 1
                binding.rbRetStar3.isChecked -> 3
                else -> 5
            }
            val notes = binding.etReturnNotes.text.toString().trim()

            store.returnAsset(
                entryId = entry.id,
                actualReturnDate = selectedActualDate,
                returnConditionRating = rating,
                notes = notes
            )

            Toast.makeText(activity, "🎉 【${entry.brand}】已成功归还打卡并恢复在库状态！", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onReturned()
        }

        dialog.show()
    }

    /** 导出并分享借条 / 催还海报 */
    private fun exportAndShareVoucher(
        activity: Activity,
        entry: Entry,
        record: LendingRecord,
        isReminderMode: Boolean
    ) {
        val actionTitle = if (isReminderMode) "催还温馨提醒海报" else "实物借出电子借条凭证"
        Toast.makeText(activity, "正在渲染 1080P $actionTitle...", Toast.LENGTH_SHORT).show()

        try {
            val bitmap = LendingVoucherGenerator.generateVoucherBitmap(activity, entry, record, isReminderMode)
            val uri = LendingVoucherGenerator.saveVoucherToGallery(activity, bitmap, entry.brand)

            if (uri != null) {
                ModernDialogHelper.showInfoDialog(
                    context = activity,
                    title = "🎉 $actionTitle 已保存至相册！",
                    emoji = "🖼️",
                    message = "已成功生成【${entry.brand}】的 1080P 高清流转凭据，并保存至系统相册 (Pictures/Collecter)！\n\n您可以随时查看或直接通过微信分享给【${record.borrowerName}】。",
                    buttonText = "🌟 查看并分享给好友"
                ) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri as android.os.Parcelable)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "分享【${entry.brand}】$actionTitle"))
                }
            } else {
                Toast.makeText(activity, "保存至系统相册失败，请检查存储权限", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "生成凭据出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
