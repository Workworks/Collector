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
import com.kfaino.diapertracker.databinding.DialogBeverageVaultBinding
import java.util.UUID

/**
 * 🍷 茶窖珍藏、酒品陈化与适饮时效控制器 (Cellar & Tea Vault Dialog)
 * - 名茶、名酒、咖啡豆存放环境与空间箱位定位
 * - 陈化年份精算、适饮黄金峰值期提醒与开封保质期追踪
 * - 一键开瓶/拆封打卡与库存快速消耗
 */
object BeverageTeaDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogBeverageVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentFilter = "all" // "all", "tea", "wine", "spirit", "coffee"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allRecords = store.getBeverageRecords()
            val peakCount = allRecords.count { it.isPeakDrinkingNow() }
            val openedCount = allRecords.count { it.isOpened() }
            val expiredCount = allRecords.count { it.isOpenExpired() }

            binding.tvStatTotalBeverages.text = "${allRecords.size} 项"
            binding.tvStatPeakBeverages.text = "$peakCount 项"
            binding.tvStatOpenedBeverages.text = "$openedCount 项"
            binding.tvStatExpiredBeverages.text = "$expiredCount 项"

            val filtered = allRecords.filter { record ->
                val matchesFilter = when (currentFilter) {
                    "tea" -> record.category.contains("茶")
                    "wine" -> record.category.contains("葡萄酒") || record.category.contains("酒庄")
                    "spirit" -> record.category.contains("烈酒") || record.category.contains("白酒") || record.category.contains("威士忌")
                    "coffee" -> record.category.contains("咖啡")
                    else -> true
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        record.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.originRegion.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.category.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.storageLocation.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.tastingNotes.contains(currentSearchKeyword, ignoreCase = true)

                matchesFilter && matchesSearch
            }.sortedWith(
                compareByDescending<BeverageTeaRecord> { it.isOpenExpired() }
                    .thenByDescending { it.isPeakDrinkingNow() }
                    .thenByDescending { it.rating }
            )

            binding.layoutBeveragesEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBeverages.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvBeverages.adapter = BeverageTeaAdapter(
                activity = activity,
                list = filtered,
                onOpenClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "🍷 开瓶/拆封品鉴确认",
                        message = "确认已开瓶/拆封【${record.name}】？\n系统将自动记录品鉴起始时间与保鲜倒计时。",
                        emoji = "🍷",
                        positiveText = "确认开瓶",
                        negativeText = "取消"
                    ) {
                        store.openBeverage(record.id)
                        Toast.makeText(activity, "🍷 【${record.name}】已成功开瓶品鉴！", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                },
                onConsumeQtyClick = { record ->
                    store.consumeBeverageQty(record.id, 1)
                    val updated = store.getBeverageRecords().firstOrNull { it.id == record.id }
                    Toast.makeText(activity, "📉 【${record.name}】库存已消耗 1 ${record.unit} (剩余: ${updated?.qty ?: 0} ${record.unit})", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { record ->
                    showAddOrEditBeverageDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出珍藏档案",
                        message = "确认移出【${record.name}】的珍藏记录？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteBeverageRecord(record.id)
                        Toast.makeText(activity, "已移出珍藏记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvBeverages.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupBeverageFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_beverage_tea -> "tea"
                R.id.chip_beverage_wine -> "wine"
                R.id.chip_beverage_spirit -> "spirit"
                R.id.chip_beverage_coffee -> "coffee"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchBeverages) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddBeverage.applyPressScaleAnimation(0.92f)
        binding.btnAddBeverage.setOnClickListener {
            showAddOrEditBeverageDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseBeverageVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseBeverageVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出登记/编辑珍藏档案弹窗 */
    fun showAddOrEditBeverageDialog(
        activity: Activity,
        store: DataStore,
        editingRecord: BeverageTeaRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_beverage, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_beverage_title)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chip_group_beverage_category_edit)
        val etName = view.findViewById<EditText>(R.id.et_beverage_name)
        val etOrigin = view.findViewById<EditText>(R.id.et_beverage_origin)
        val etVintage = view.findViewById<EditText>(R.id.et_beverage_vintage)
        val etBestYear = view.findViewById<EditText>(R.id.et_beverage_best_year)
        val etQty = view.findViewById<EditText>(R.id.et_beverage_qty)
        val etUnit = view.findViewById<EditText>(R.id.et_beverage_unit)
        val etOpenLifeDays = view.findViewById<EditText>(R.id.et_beverage_open_life_days)
        val etLocation = view.findViewById<EditText>(R.id.et_beverage_location)
        val etRating = view.findViewById<EditText>(R.id.et_beverage_rating)
        val etNotes = view.findViewById<EditText>(R.id.et_beverage_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_beverage)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_beverage)

        if (editingRecord != null) {
            tvTitle.text = "🍷 编辑名茶名酿档案"
            when {
                editingRecord.category.contains("葡萄酒") || editingRecord.category.contains("酒庄") ->
                    chipGroupCategory.check(R.id.chip_edit_cat_wine)
                editingRecord.category.contains("烈酒") || editingRecord.category.contains("白酒") || editingRecord.category.contains("威士忌") ->
                    chipGroupCategory.check(R.id.chip_edit_cat_spirit)
                editingRecord.category.contains("咖啡") ->
                    chipGroupCategory.check(R.id.chip_edit_cat_coffee)
                editingRecord.category.contains("其它") ->
                    chipGroupCategory.check(R.id.chip_edit_cat_other)
                else -> chipGroupCategory.check(R.id.chip_edit_cat_tea)
            }
            etName.setText(editingRecord.name)
            etOrigin.setText(editingRecord.originRegion)
            etVintage.setText("${editingRecord.vintageYear}")
            etBestYear.setText("${editingRecord.bestDrinkingYear}")
            etQty.setText("${editingRecord.qty}")
            etUnit.setText(editingRecord.unit)
            etOpenLifeDays.setText("${editingRecord.openShelfLifeDays}")
            etLocation.setText(editingRecord.storageLocation)
            etRating.setText("${editingRecord.rating}")
            etNotes.setText(editingRecord.tastingNotes)
        } else {
            tvTitle.text = "🍷 登记茶窖与名酿珍藏"
            chipGroupCategory.check(R.id.chip_edit_cat_tea)
            etVintage.setText("2020")
            etBestYear.setText("2030")
            etQty.setText("1")
            etUnit.setText("瓶")
            etOpenLifeDays.setText("0")
            etRating.setText("5.0")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入珍藏品名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val origin = etOrigin.text.toString().trim()
            val category = when (chipGroupCategory.checkedChipId) {
                R.id.chip_edit_cat_wine -> "葡萄酒庄"
                R.id.chip_edit_cat_spirit -> "烈酒名酿"
                R.id.chip_edit_cat_coffee -> "精品咖啡"
                R.id.chip_edit_cat_other -> "其它佳酿"
                else -> "茶品干货"
            }

            val vintage = etVintage.text.toString().toIntOrNull() ?: 2020
            val bestYear = etBestYear.text.toString().toIntOrNull() ?: 2030
            val qty = etQty.text.toString().toIntOrNull() ?: 1
            val unit = etUnit.text.toString().trim().ifBlank { "瓶" }
            val openLifeDays = etOpenLifeDays.text.toString().toIntOrNull() ?: 0
            val location = etLocation.text.toString().trim()
            val rating = etRating.text.toString().toFloatOrNull() ?: 5.0f
            val notes = etNotes.text.toString().trim()

            val record = editingRecord?.copy(
                name = name,
                category = category,
                vintageYear = vintage,
                originRegion = origin,
                storageLocation = location,
                qty = qty,
                unit = unit,
                bestDrinkingYear = bestYear,
                openShelfLifeDays = openLifeDays,
                rating = rating,
                tastingNotes = notes
            ) ?: BeverageTeaRecord(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                vintageYear = vintage,
                originRegion = origin,
                storageLocation = location,
                qty = qty,
                unit = unit,
                bestDrinkingYear = bestYear,
                openShelfLifeDays = openLifeDays,
                rating = rating,
                tastingNotes = notes
            )

            store.addOrUpdateBeverageRecord(record)
            Toast.makeText(activity, "🎉 【$name】已成功纳入茶窖酒品珍藏舱！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
