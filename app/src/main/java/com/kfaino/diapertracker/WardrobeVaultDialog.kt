package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogWardrobeVaultBinding
import java.util.UUID

/**
 * 👗 换季衣橱、四季穿搭与封箱收纳舱控制器 (Wardrobe & Seasonal Closet Dialog)
 * - 支持四季胶囊衣橱分舱（春秋 / 夏季 / 冬季 / 四季通用）
 * - 支持换季真空压缩袋/箱盒封箱状态一键打卡与解封入柜
 * - 支持穿着出镜打卡、次均穿戴成本精算与 180 天沉睡未穿预警
 */
object WardrobeVaultDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogWardrobeVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentSeasonFilter = "all" // "all", "spring_autumn", "summer", "winter", "sealed"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allWardrobes = store.getWardrobeRecords()
            val sealedCount = allWardrobes.count { it.isSealed }
            val sleepingCount = allWardrobes.count { it.isSleeping() }

            binding.tvStatTotalWardrobe.text = "${allWardrobes.size} 件"
            binding.tvStatSealedWardrobe.text = "$sealedCount 件"
            binding.tvStatSleepingWardrobe.text = "$sleepingCount 件"

            val filtered = allWardrobes.filter { item ->
                val matchesSeason = when (currentSeasonFilter) {
                    "all" -> true
                    "sealed" -> item.isSealed
                    else -> item.season == currentSeasonFilter && !item.isSealed
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        item.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.color.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.material.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.storageLocation.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.careNotes.contains(currentSearchKeyword, ignoreCase = true) ||
                        item.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesSeason && matchesSearch
            }.sortedWith(
                compareByDescending<WardrobeRecord> { it.wearCount }
                    .thenByDescending { it.purchaseDate }
            )

            binding.layoutWardrobeEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvWardrobe.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvWardrobe.adapter = WardrobeAdapter(
                activity = activity,
                list = filtered,
                onWearClick = { item ->
                    store.markWardrobeWorn(item.id)
                    Toast.makeText(activity, "✨ 【${item.name}】穿着打卡成功 (+1次)", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onToggleSealClick = { item ->
                    val actionName = if (item.isSealed) "解封入柜" else "换季封箱"
                    store.toggleWardrobeSealed(item.id)
                    Toast.makeText(activity, "📦 已将【${item.name}】设为$actionName", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { item ->
                    showAddOrEditWardrobeDialog(activity, store, item) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { item ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出衣橱单品",
                        message = "确认从衣橱中彻底移出【${item.name}】？",
                        emoji = "🗑️",
                        positiveText = "移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteWardrobeRecord(item.id)
                        Toast.makeText(activity, "已移出单品记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvWardrobe.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupWardrobeSeasons.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSeasonFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_season_spring -> "spring_autumn"
                R.id.chip_season_summer -> "summer"
                R.id.chip_season_winter -> "winter"
                R.id.chip_season_sealed -> "sealed"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchWardrobe) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddWardrobe.applyPressScaleAnimation(0.92f)
        binding.btnAddWardrobe.setOnClickListener {
            showAddOrEditWardrobeDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseWardrobeVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseWardrobeVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出添加/编辑服饰单品弹窗 */
    fun showAddOrEditWardrobeDialog(
        activity: Activity,
        store: DataStore,
        editingItem: WardrobeRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_wardrobe, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_wardrobe_title)
        val chipGroupSeason = view.findViewById<ChipGroup>(R.id.chip_group_wardrobe_season_edit)
        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chip_group_wardrobe_cat_edit)
        val etName = view.findViewById<EditText>(R.id.et_wardrobe_name)
        val etColor = view.findViewById<EditText>(R.id.et_wardrobe_color)
        val etMaterial = view.findViewById<EditText>(R.id.et_wardrobe_material)
        val etLocation = view.findViewById<EditText>(R.id.et_wardrobe_location)
        val etPrice = view.findViewById<EditText>(R.id.et_wardrobe_price)
        val etCare = view.findViewById<EditText>(R.id.et_wardrobe_care)
        val cbIsSealed = view.findViewById<CheckBox>(R.id.cb_wardrobe_is_sealed)
        val etNotes = view.findViewById<EditText>(R.id.et_wardrobe_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_wardrobe)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_wardrobe)

        if (editingItem != null) {
            tvTitle.text = "👗 编辑服饰单品"
            when (editingItem.season) {
                "spring_autumn" -> chipGroupSeason.check(R.id.chip_edit_season_spring)
                "summer" -> chipGroupSeason.check(R.id.chip_edit_season_summer)
                "all_season" -> chipGroupSeason.check(R.id.chip_edit_season_all)
                else -> chipGroupSeason.check(R.id.chip_edit_season_winter)
            }
            when (editingItem.category) {
                "top" -> chipGroupCat.check(R.id.chip_edit_cat_top)
                "pants" -> chipGroupCat.check(R.id.chip_edit_cat_pants)
                "shoes" -> chipGroupCat.check(R.id.chip_edit_cat_shoes)
                "accessory" -> chipGroupCat.check(R.id.chip_edit_cat_accessory)
                "bedding" -> chipGroupCat.check(R.id.chip_edit_cat_bedding)
                else -> chipGroupCat.check(R.id.chip_edit_cat_coat)
            }
            etName.setText(editingItem.name)
            etColor.setText(editingItem.color)
            etMaterial.setText(editingItem.material)
            etLocation.setText(editingItem.storageLocation)
            if (editingItem.purchasePrice > 0.0) {
                etPrice.setText("${editingItem.purchasePrice}")
            }
            etCare.setText(editingItem.careNotes)
            cbIsSealed.isChecked = editingItem.isSealed
            etNotes.setText(editingItem.notes)
        } else {
            tvTitle.text = "👗 添置服饰单品"
            chipGroupSeason.check(R.id.chip_edit_season_winter)
            chipGroupCat.check(R.id.chip_edit_cat_coat)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入单品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSeason = when (chipGroupSeason.checkedChipId) {
                R.id.chip_edit_season_spring -> "spring_autumn"
                R.id.chip_edit_season_summer -> "summer"
                R.id.chip_edit_season_all -> "all_season"
                else -> "winter"
            }

            val selectedCat = when (chipGroupCat.checkedChipId) {
                R.id.chip_edit_cat_top -> "top"
                R.id.chip_edit_cat_pants -> "pants"
                R.id.chip_edit_cat_shoes -> "shoes"
                R.id.chip_edit_cat_accessory -> "accessory"
                R.id.chip_edit_cat_bedding -> "bedding"
                else -> "coat"
            }

            val color = etColor.text.toString().trim()
            val material = etMaterial.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val care = etCare.text.toString().trim()
            val isSealed = cbIsSealed.isChecked
            val notes = etNotes.text.toString().trim()

            val record = editingItem?.copy(
                name = name,
                season = selectedSeason,
                category = selectedCat,
                color = color,
                material = material,
                storageLocation = location,
                purchasePrice = price,
                careNotes = care,
                isSealed = isSealed,
                sealedAt = if (isSealed && editingItem.sealedAt == 0L) System.currentTimeMillis() else (if (isSealed) editingItem.sealedAt else 0L),
                notes = notes
            ) ?: WardrobeRecord(
                id = UUID.randomUUID().toString(),
                name = name,
                season = selectedSeason,
                category = selectedCat,
                color = color,
                material = material,
                storageLocation = location,
                purchasePrice = price,
                purchaseDate = System.currentTimeMillis(),
                wearCount = 0,
                lastWornAt = 0L,
                isSealed = isSealed,
                sealedAt = if (isSealed) System.currentTimeMillis() else 0L,
                notes = notes
            )

            store.addOrUpdateWardrobeRecord(record)
            Toast.makeText(activity, "🎉 【$name】已收入换季衣橱！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
