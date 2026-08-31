package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogEmergencyVaultBinding
import java.util.Date
import java.util.UUID

/**
 * 🚨 家庭应急防灾、避难包与生命线物资控制器 (Emergency & Survival Vault Dialog)
 * - 支持四大应急避难专包（地震避险72h / 火灾逃生 / 车载救援 / 暴雨防汛）
 * - 支持物资保质期、自放电测试与滤毒失效生命线追踪
 * - 支持安全演练季度点检打卡与黄金动线存放定位
 */
object EmergencyVaultDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogEmergencyVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentKitFilter = "all" // "all", "earthquake", "fire", "car", "flood"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allItems = store.getEmergencyItems()
            val expiredCount = allItems.count { it.isExpired() }
            val expiringCount = allItems.count { it.isExpiringSoon() || it.isNeedsCheck() }

            binding.tvStatTotalEmergency.text = "${allItems.size} 件"
            binding.tvStatExpiringEmergency.text = "$expiringCount 项"
            binding.tvStatExpiredEmergency.text = "$expiredCount 项"

            val filtered = allItems.filter { item ->
                val matchesKit = when (currentKitFilter) {
                    "all" -> true
                    else -> item.kitType == currentKitFilter
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        item.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.location.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesKit && matchesSearch
            }.sortedWith(
                compareByDescending<EmergencyItem> { it.isExpired() }
                    .thenByDescending { it.isExpiringSoon() }
                    .thenByDescending { it.isNeedsCheck() }
            )

            binding.layoutEmergencyEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvEmergency.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvEmergency.adapter = EmergencyAdapter(
                activity = activity,
                list = filtered,
                onCheckClick = { item ->
                    store.checkEmergencyItem(item.id)
                    Toast.makeText(activity, "✅ 已完成【${item.name}】安全点检演练打卡！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { item ->
                    showAddOrEditEmergencyDialog(activity, store, item) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { item ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出应急物资",
                        message = "确认移出【${item.name}】？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteEmergencyItem(item.id)
                        Toast.makeText(activity, "已移出应急物资记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvEmergency.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupEmergencyKits.setOnCheckedStateChangeListener { _, checkedIds ->
            currentKitFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_kit_earthquake -> "earthquake"
                R.id.chip_kit_fire -> "fire"
                R.id.chip_kit_car -> "car"
                R.id.chip_kit_flood -> "flood"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchEmergency) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddEmergency.applyPressScaleAnimation(0.92f)
        binding.btnAddEmergency.setOnClickListener {
            showAddOrEditEmergencyDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseEmergencyVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseEmergencyVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出添加/编辑应急物资弹窗 */
    fun showAddOrEditEmergencyDialog(
        activity: Activity,
        store: DataStore,
        editingItem: EmergencyItem?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_emergency, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_emergency_title)
        val chipGroupKit = view.findViewById<ChipGroup>(R.id.chip_group_emergency_kit_edit)
        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chip_group_emergency_cat_edit)
        val etName = view.findViewById<EditText>(R.id.et_emergency_name)
        val etQty = view.findViewById<EditText>(R.id.et_emergency_qty)
        val etUnit = view.findViewById<EditText>(R.id.et_emergency_unit)
        val etLocation = view.findViewById<EditText>(R.id.et_emergency_location)
        val btnPickExpiry = view.findViewById<View>(R.id.btn_pick_emergency_expiry)
        val tvExpiryText = view.findViewById<TextView>(R.id.tv_emergency_expiry_text)
        val etRotMonths = view.findViewById<EditText>(R.id.et_emergency_rotation_months)
        val etNotes = view.findViewById<EditText>(R.id.et_emergency_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_emergency)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_emergency)

        var selectedExpiryDate = editingItem?.expiryDate ?: 0L

        fun updateExpiryUi() {
            if (selectedExpiryDate > 0L) {
                tvExpiryText.text = "📅 保质/失效截止: ${VaultUiHelper.standardDateFormat.format(Date(selectedExpiryDate))}"
                tvExpiryText.setTextColor(activity.getColor(R.color.primary))
            } else {
                tvExpiryText.text = "📅 设为长期耐用 / 暂不设限"
                tvExpiryText.setTextColor(activity.getColor(R.color.text_secondary))
            }
        }

        if (editingItem != null) {
            tvTitle.text = "🚨 编辑应急防灾物资"
            when (editingItem.kitType) {
                "fire" -> chipGroupKit.check(R.id.chip_edit_kit_fire)
                "car" -> chipGroupKit.check(R.id.chip_edit_kit_car)
                "flood" -> chipGroupKit.check(R.id.chip_edit_kit_flood)
                "general" -> chipGroupKit.check(R.id.chip_edit_kit_general)
                else -> chipGroupKit.check(R.id.chip_edit_kit_earthquake)
            }
            when (editingItem.category) {
                "food_water" -> chipGroupCat.check(R.id.chip_edit_cat_food)
                "medical" -> chipGroupCat.check(R.id.chip_edit_cat_medical)
                "protection" -> chipGroupCat.check(R.id.chip_edit_cat_protection)
                else -> chipGroupCat.check(R.id.chip_edit_cat_tool)
            }
            etName.setText(editingItem.name)
            etQty.setText(if (editingItem.qty % 1.0 == 0.0) editingItem.qty.toInt().toString() else editingItem.qty.toString())
            etUnit.setText(editingItem.unit)
            etLocation.setText(editingItem.location)
            if (editingItem.rotationIntervalMonths > 0) {
                etRotMonths.setText("${editingItem.rotationIntervalMonths}")
            }
            etNotes.setText(editingItem.notes)
            updateExpiryUi()
        } else {
            tvTitle.text = "🚨 备入应急防灾物资"
            chipGroupKit.check(R.id.chip_edit_kit_earthquake)
            chipGroupCat.check(R.id.chip_edit_cat_tool)
            etUnit.setText("件")
            updateExpiryUi()
        }

        btnPickExpiry.setOnClickListener {
            VaultUiHelper.showDatePicker(activity, selectedExpiryDate) { time, _ ->
                selectedExpiryDate = time
                updateExpiryUi()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入物资名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedKit = when (chipGroupKit.checkedChipId) {
                R.id.chip_edit_kit_fire -> "fire"
                R.id.chip_edit_kit_car -> "car"
                R.id.chip_edit_kit_flood -> "flood"
                R.id.chip_edit_kit_general -> "general"
                else -> "earthquake"
            }

            val selectedCat = when (chipGroupCat.checkedChipId) {
                R.id.chip_edit_cat_food -> "food_water"
                R.id.chip_edit_cat_medical -> "medical"
                R.id.chip_edit_cat_protection -> "protection"
                else -> "tool"
            }

            val qty = etQty.text.toString().toDoubleOrNull() ?: 1.0
            val unit = etUnit.text.toString().trim().ifBlank { "件" }
            val location = etLocation.text.toString().trim()
            val rotMonths = etRotMonths.text.toString().toIntOrNull() ?: 0
            val notes = etNotes.text.toString().trim()

            val record = editingItem?.copy(
                name = name,
                kitType = selectedKit,
                category = selectedCat,
                qty = qty,
                unit = unit,
                location = location,
                expiryDate = selectedExpiryDate,
                rotationIntervalMonths = rotMonths,
                notes = notes
            ) ?: EmergencyItem(
                id = UUID.randomUUID().toString(),
                name = name,
                kitType = selectedKit,
                category = selectedCat,
                qty = qty,
                unit = unit,
                location = location,
                expiryDate = selectedExpiryDate,
                rotationIntervalMonths = rotMonths,
                lastCheckedAt = System.currentTimeMillis(),
                notes = notes
            )

            store.addOrUpdateEmergencyItem(record)
            Toast.makeText(activity, "🎉 【$name】已纳入家庭应急生命线！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
