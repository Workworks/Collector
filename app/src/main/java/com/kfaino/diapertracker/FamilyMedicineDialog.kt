package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogAddMedicineBinding
import com.kfaino.diapertracker.databinding.DialogFamilyMedicineBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 💊 家庭智能健康药箱控制器 (Family Medicine Dialog Controller)
 * - 按病症对症速查（发热/感冒/肠胃/外伤/抗敏）
 * - 用法用量清晰展示、开封打卡与保质期/开封时效严密守护
 */
object FamilyMedicineDialog {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 打开家庭健康药箱主弹窗 */
    fun showMedicineVaultDialog(
        activity: Activity,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogFamilyMedicineBinding.inflate(activity.layoutInflater)
        var currentCategoryFilter = "all" // "all", "fever", "cold", "digest", "trauma", "allergy"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        fun refreshList() {
            val all = store.getMedicines()
            val expiredList = all.filter { it.isExpired() }
            val expiringList = all.filter { !it.isExpired() && (it.getEffectiveExpiryDate() - System.currentTimeMillis()) in 0..(30L * 24 * 60 * 60 * 1000) }

            // 统计看板
            binding.tvMedicineTotalCount.text = "${all.size} 种"
            binding.tvMedicineExpiringCount.text = "${expiringList.size} 项"
            binding.tvMedicineExpiredCount.text = "${expiredList.size} 种"

            val displayList = if (currentCategoryFilter == "all") {
                all
            } else {
                all.filter { it.category == currentCategoryFilter }
            }

            renderMedicineList(activity, store, displayList, currentCategoryFilter, binding.medicineListContainer) {
                refreshList()
                onUpdated()
            }
        }

        fun updateTabs() {
            val activeBg = R.drawable.bg_chip_active
            val inActiveBg = R.drawable.bg_chip_inactive
            val white = Color.WHITE
            val secColor = ContextCompat.getColor(activity, R.color.text_secondary)

            binding.tabMedAll.setBackgroundResource(if (currentCategoryFilter == "all") activeBg else inActiveBg)
            binding.tabMedAll.setTextColor(if (currentCategoryFilter == "all") white else secColor)

            binding.tabMedFever.setBackgroundResource(if (currentCategoryFilter == "fever") activeBg else inActiveBg)
            binding.tabMedFever.setTextColor(if (currentCategoryFilter == "fever") white else secColor)

            binding.tabMedCold.setBackgroundResource(if (currentCategoryFilter == "cold") activeBg else inActiveBg)
            binding.tabMedCold.setTextColor(if (currentCategoryFilter == "cold") white else secColor)

            binding.tabMedDigest.setBackgroundResource(if (currentCategoryFilter == "digest") activeBg else inActiveBg)
            binding.tabMedDigest.setTextColor(if (currentCategoryFilter == "digest") white else secColor)

            binding.tabMedTrauma.setBackgroundResource(if (currentCategoryFilter == "trauma") activeBg else inActiveBg)
            binding.tabMedTrauma.setTextColor(if (currentCategoryFilter == "trauma") white else secColor)

            binding.tabMedAllergy.setBackgroundResource(if (currentCategoryFilter == "allergy") activeBg else inActiveBg)
            binding.tabMedAllergy.setTextColor(if (currentCategoryFilter == "allergy") white else secColor)
        }

        binding.tabMedAll.setOnClickListener { currentCategoryFilter = "all"; updateTabs(); refreshList() }
        binding.tabMedFever.setOnClickListener { currentCategoryFilter = "fever"; updateTabs(); refreshList() }
        binding.tabMedCold.setOnClickListener { currentCategoryFilter = "cold"; updateTabs(); refreshList() }
        binding.tabMedDigest.setOnClickListener { currentCategoryFilter = "digest"; updateTabs(); refreshList() }
        binding.tabMedTrauma.setOnClickListener { currentCategoryFilter = "trauma"; updateTabs(); refreshList() }
        binding.tabMedAllergy.setOnClickListener { currentCategoryFilter = "allergy"; updateTabs(); refreshList() }

        binding.btnCloseMedicineVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseMedicineVault.setOnClickListener { dialog.dismiss() }

        binding.btnOpenAddMedicine.applyPressScaleAnimation(0.92f)
        binding.btnOpenAddMedicine.setOnClickListener {
            showAddOrEditMedicineDialog(activity, store, medicine = null) {
                refreshList()
                onUpdated()
            }
        }

        refreshList()
        dialog.show()
    }

    /** 动态渲染药品卡片列表 */
    private fun renderMedicineList(
        activity: Activity,
        store: DataStore,
        list: List<MedicineRecord>,
        currentFilter: String,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (list.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = "💊 暂无相关药品记录\n点击下方按钮立即登记家庭常备急救药吧~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyTv)
            return
        }

        for (m in list) {
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

            // 1. 顶行：分类 + 药名 + 剂型 + 过期状态标签
            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val catBadge = TextView(activity).apply {
                text = m.getCategoryDisplayName()
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(6), dp(2), dp(6), dp(2))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                }
            }

            val nameTv = TextView(activity).apply {
                text = "${m.name} (${m.form})"
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val statusBadge = TextView(activity).apply {
                val isExp = m.isExpired()
                text = if (isExp) "🔴 严禁服用" else if (m.isOpened) "📦 已开封" else "🟢 在库"
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(if (isExp) ContextCompat.getColor(activity, R.color.danger) else if (m.isOpened) ContextCompat.getColor(activity, R.color.accent_dark) else ContextCompat.getColor(activity, R.color.text_secondary))
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(6), dp(2), dp(6), dp(2))
            }

            topRow.addView(catBadge)
            topRow.addView(nameTv)
            topRow.addView(statusBadge)
            root.addView(topRow)

            // 2. 用法用量与存放位置
            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(6), 0, dp(4))
            }

            if (m.dosage.isNotBlank()) {
                val dosageTv = TextView(activity).apply {
                    text = "📋 用法用量: ${m.dosage}"
                    textSize = 13f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                }
                infoLayout.addView(dosageTv)
            }

            val locStr = if (m.location.isNotBlank()) "📍 存放: ${m.location}" else ""
            val expDesc = m.getExpiryStatusText()
            val expiryTv = TextView(activity).apply {
                text = "$expDesc   $locStr"
                textSize = 12f
                setTextColor(if (m.isExpired()) ContextCompat.getColor(activity, R.color.danger) else ContextCompat.getColor(activity, R.color.text_secondary))
            }
            infoLayout.addView(expiryTv)

            if (m.contraindications.isNotBlank()) {
                val contraTv = TextView(activity).apply {
                    text = "⚠️ 注意事项: ${m.contraindications}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.accent_dark))
                }
                infoLayout.addView(contraTv)
            }

            root.addView(infoLayout)

            // 3. 底部快捷操作栏
            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(4), 0, 0)
            }

            if (!m.isOpened && !m.isExpired()) {
                val btnOpen = TextView(activity).apply {
                    text = "📦 开封打卡"
                    textSize = 12f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(activity, R.color.primary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        store.markMedicineOpened(m.id)
                        Toast.makeText(activity, "🎉 【${m.name}】已开封打卡并启动时效追踪！", Toast.LENGTH_SHORT).show()
                        onRefreshNeeded()
                    }
                }
                btnRow.addView(btnOpen)
            }

            val btnEdit = TextView(activity).apply {
                text = "✏️ 编辑"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    showAddOrEditMedicineDialog(activity, store, m) {
                        onRefreshNeeded()
                    }
                }
            }

            val btnDelete = TextView(activity).apply {
                text = if (m.isExpired()) "🗑️ 环保丢弃" else "🗑️ 删除"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.danger))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "确认丢弃/删除药品？",
                        message = "确定要从药箱移除【${m.name}】吗？",
                        emoji = "🗑️",
                        positiveText = "确认移除",
                        negativeText = "取消"
                    ) {
                        store.deleteMedicine(m.id)
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

    /** 登记 / 编辑常备药弹窗 */
    fun showAddOrEditMedicineDialog(
        activity: Activity,
        store: DataStore,
        medicine: MedicineRecord?,
        onSaved: () -> Unit
    ) {
        val binding = DialogAddMedicineBinding.inflate(activity.layoutInflater)
        var selectedCategory = medicine?.category ?: "fever"
        var selectedForm = medicine?.form ?: "胶囊"
        var selectedExpiryDate = medicine?.expiryDate ?: (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val categoryList = listOf(
            "fever" to "🤒 发烧镇痛",
            "cold" to "🤧 感冒咳嗽",
            "digest" to "🤢 肠胃消化",
            "trauma" to "🩹 外伤消炎",
            "allergy" to "🌿 抗过敏",
            "chronic" to "💊 慢病常备",
            "other" to "📦 其他常备"
        )
        val formList = listOf("胶囊", "片剂", "颗粒/冲剂", "口服液", "外用喷剂/眼药水", "敷料/贴膏", "其他")

        fun updateCategoryText() {
            val name = categoryList.firstOrNull { it.first == selectedCategory }?.second ?: "🤒 分类"
            binding.tvSelectMedCategory.text = name
        }
        fun updateFormText() {
            binding.tvSelectMedForm.text = selectedForm
        }
        fun updateExpiryText() {
            if (selectedExpiryDate > 0L) {
                binding.tvMedExpiryPicker.text = "📅 保质期截止: " + dateFormat.format(Date(selectedExpiryDate))
            } else {
                binding.tvMedExpiryPicker.text = "📅 长期有效 (点击选择截止日)"
            }
        }

        updateCategoryText()
        updateFormText()
        updateExpiryText()

        binding.tvSelectMedCategory.setOnClickListener {
            val names = categoryList.map { it.second }
            val currentIdx = categoryList.indexOfFirst { it.first == selectedCategory }.coerceAtLeast(0)
            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择对症功效分类",
                emoji = "💊",
                options = names,
                selectedIndex = currentIdx
            ) { which, _ ->
                selectedCategory = categoryList[which].first
                updateCategoryText()
            }
        }

        binding.tvSelectMedForm.setOnClickListener {
            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择药品剂型",
                emoji = "💊",
                options = formList,
                selectedIndex = formList.indexOf(selectedForm).coerceAtLeast(0)
            ) { which, _ ->
                selectedForm = formList[which]
                updateFormText()
            }
        }

        binding.tvMedExpiryPicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, if (selectedExpiryDate > 0) selectedExpiryDate else System.currentTimeMillis(), "选择药品保质期截止日") { timeMs ->
                selectedExpiryDate = timeMs
                updateExpiryText()
            }
        }

        if (medicine != null) {
            binding.tvAddMedDialogTitle.text = "✏️ 编辑药品档案"
            binding.etMedName.setText(medicine.name)
            binding.etMedDosage.setText(medicine.dosage)
            binding.etMedLocation.setText(medicine.location)
            binding.etMedOpenedDays.setText(if (medicine.openedValidityDays > 0) medicine.openedValidityDays.toString() else "")
            binding.etMedContra.setText(medicine.contraindications)
        }

        binding.btnCloseAddMed.applyPressScaleAnimation(0.92f)
        binding.btnCloseAddMed.setOnClickListener { dialog.dismiss() }

        binding.btnCancelMed.applyPressScaleAnimation(0.92f)
        binding.btnCancelMed.setOnClickListener { dialog.dismiss() }

        binding.btnConfirmMed.applyPressScaleAnimation(0.92f)
        binding.btnConfirmMed.setOnClickListener {
            val name = binding.etMedName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入药品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dosage = binding.etMedDosage.text.toString().trim()
            val location = binding.etMedLocation.text.toString().trim().ifBlank { "家庭急救药箱" }
            val openedDays = binding.etMedOpenedDays.text.toString().toIntOrNull() ?: 0
            val contra = binding.etMedContra.text.toString().trim()

            val record = MedicineRecord(
                id = medicine?.id ?: UUID.randomUUID().toString(),
                name = name,
                category = selectedCategory,
                form = selectedForm,
                qty = medicine?.qty ?: 1,
                unit = medicine?.unit ?: "盒",
                location = location,
                dosage = dosage,
                targetAudience = medicine?.targetAudience ?: "全家通用",
                expiryDate = selectedExpiryDate,
                isOpened = medicine?.isOpened ?: false,
                openedAt = medicine?.openedAt ?: 0L,
                openedValidityDays = openedDays,
                photoPath = medicine?.photoPath ?: "",
                contraindications = contra
            )

            store.addOrUpdateMedicine(record)
            Toast.makeText(activity, "🎉 药品【$name】已成功收纳进家庭药箱！", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
