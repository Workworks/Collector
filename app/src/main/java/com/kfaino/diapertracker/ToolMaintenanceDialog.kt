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
import com.kfaino.diapertracker.databinding.DialogToolVaultBinding
import java.util.UUID

/**
 * 🔧 家庭工具、五金配件与设备维保控制器 (Tool Maintenance Vault Dialog)
 * - 支持四大工具类别（⚡ 电动工具 / 🔨 手工工具 / 🔩 五金耗材 / 🛠️ 设备维保）
 * - 支持螺丝五金与钻头搭配参数速查
 * - 支持净水器/新风滤网等周期性设备维保排期追踪与一键打卡
 */
object ToolMaintenanceDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogToolVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentCategoryFilter = "all" // "all", "power_tool", "hand_tool", "hardware", "maintenance"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allRecords = store.getToolRecords()
            val overdueCount = allRecords.count { it.isMaintenanceDue() }
            val expiringCount = allRecords.count { it.isMaintenanceDueSoon() }

            binding.tvStatTotalTools.text = "${allRecords.size} 件"
            binding.tvStatExpiringTools.text = "$expiringCount 项"
            binding.tvStatOverdueTools.text = "$overdueCount 项"

            val filtered = allRecords.filter { record ->
                val matchesCategory = when (currentCategoryFilter) {
                    "all" -> true
                    else -> record.category == currentCategoryFilter
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        record.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.spec.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.location.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesCategory && matchesSearch
            }.sortedWith(
                compareByDescending<ToolMaintenanceRecord> { it.isMaintenanceDue() }
                    .thenByDescending { it.isMaintenanceDueSoon() }
            )

            binding.layoutToolsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvTools.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvTools.adapter = ToolMaintenanceAdapter(
                activity = activity,
                list = filtered,
                onMaintainClick = { record ->
                    store.markToolMaintained(record.id)
                    Toast.makeText(activity, "✅ 已完成【${record.name}】维保打卡，下次排期已更新！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { record ->
                    showAddOrEditToolDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出工具五金",
                        message = "确认移出【${record.name}】？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteToolRecord(record.id)
                        Toast.makeText(activity, "已移出工具记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvTools.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupToolCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategoryFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_tool_power -> "power_tool"
                R.id.chip_tool_hand -> "hand_tool"
                R.id.chip_tool_hardware -> "hardware"
                R.id.chip_tool_maint -> "maintenance"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchTools) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddTool.applyPressScaleAnimation(0.92f)
        binding.btnAddTool.setOnClickListener {
            showAddOrEditToolDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseToolVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseToolVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出录入/编辑工具五金弹窗 */
    fun showAddOrEditToolDialog(
        activity: Activity,
        store: DataStore,
        editingRecord: ToolMaintenanceRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_tool, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_tool_title)
        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chip_group_tool_cat_edit)
        val etName = view.findViewById<EditText>(R.id.et_tool_name)
        val etSpec = view.findViewById<EditText>(R.id.et_tool_spec)
        val etQty = view.findViewById<EditText>(R.id.et_tool_qty)
        val etUnit = view.findViewById<EditText>(R.id.et_tool_unit)
        val etLocation = view.findViewById<EditText>(R.id.et_tool_location)
        val etIntervalDays = view.findViewById<EditText>(R.id.et_tool_interval_days)
        val etNotes = view.findViewById<EditText>(R.id.et_tool_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_tool)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_tool)

        if (editingRecord != null) {
            tvTitle.text = "🔧 编辑工具五金与配件"
            when (editingRecord.category) {
                "hand_tool" -> chipGroupCat.check(R.id.chip_edit_cat_hand)
                "hardware" -> chipGroupCat.check(R.id.chip_edit_cat_hardware)
                "maintenance" -> chipGroupCat.check(R.id.chip_edit_cat_maint)
                else -> chipGroupCat.check(R.id.chip_edit_cat_power)
            }
            etName.setText(editingRecord.name)
            etSpec.setText(editingRecord.spec)
            etQty.setText(if (editingRecord.qty % 1.0 == 0.0) editingRecord.qty.toInt().toString() else editingRecord.qty.toString())
            etUnit.setText(editingRecord.unit)
            etLocation.setText(editingRecord.location)
            if (editingRecord.maintenanceIntervalDays > 0) {
                etIntervalDays.setText("${editingRecord.maintenanceIntervalDays}")
            }
            etNotes.setText(editingRecord.notes)
        } else {
            tvTitle.text = "🔧 录入工具五金与配件"
            chipGroupCat.check(R.id.chip_edit_cat_power)
            etUnit.setText("件")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入工具名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCat = when (chipGroupCat.checkedChipId) {
                R.id.chip_edit_cat_hand -> "hand_tool"
                R.id.chip_edit_cat_hardware -> "hardware"
                R.id.chip_edit_cat_maint -> "maintenance"
                else -> "power_tool"
            }

            val spec = etSpec.text.toString().trim()
            val qty = etQty.text.toString().toDoubleOrNull() ?: 1.0
            val unit = etUnit.text.toString().trim().ifBlank { "件" }
            val location = etLocation.text.toString().trim()
            val intervalDays = etIntervalDays.text.toString().toIntOrNull() ?: 0
            val notes = etNotes.text.toString().trim()

            val record = editingRecord?.copy(
                name = name,
                category = selectedCat,
                spec = spec,
                qty = qty,
                unit = unit,
                location = location,
                maintenanceIntervalDays = intervalDays,
                notes = notes
            ) ?: ToolMaintenanceRecord(
                id = UUID.randomUUID().toString(),
                name = name,
                category = selectedCat,
                spec = spec,
                qty = qty,
                unit = unit,
                location = location,
                maintenanceIntervalDays = intervalDays,
                lastMaintainedAt = if (intervalDays > 0) System.currentTimeMillis() else 0L,
                notes = notes
            )

            store.addOrUpdateToolRecord(record)
            Toast.makeText(activity, "🎉 【$name】已录入家庭工具五金舱！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
