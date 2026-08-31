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
import com.kfaino.diapertracker.databinding.DialogPlantVaultBinding
import java.util.UUID

/**
 * 🪴 家庭绿植花卉与水肥养护控制器 (Plant Care Vault Dialog)
 * - 支持绿植光照需求档案与摆放位置管理
 * - 支持水肥日历排期计算、临期/逾期提醒
 * - 一键浇水/施肥打卡
 */
object PlantCareDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogPlantVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentFilter = "all" // "all", "water_due", "fert_due", "full_sun", "semi_shade"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allRecords = store.getPlantRecords()
            val waterDueCount = allRecords.count { it.isWaterDue() }
            val fertDueCount = allRecords.count { it.isFertilizeDue() }

            binding.tvStatTotalPlants.text = "${allRecords.size} 盆"
            binding.tvStatWaterDuePlants.text = "$waterDueCount 盆"
            binding.tvStatFertDuePlants.text = "$fertDueCount 盆"

            val filtered = allRecords.filter { record ->
                val matchesFilter = when (currentFilter) {
                    "water_due" -> record.isWaterDue()
                    "fert_due" -> record.isFertilizeDue()
                    "full_sun" -> record.lightDemand == "full_sun"
                    "semi_shade" -> record.lightDemand == "semi_shade"
                    else -> true
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        record.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.species.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.location.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.careTips.contains(currentSearchKeyword, ignoreCase = true)

                matchesFilter && matchesSearch
            }.sortedWith(
                compareByDescending<PlantCareRecord> { it.isWaterDue() }
                    .thenByDescending { it.isFertilizeDue() }
            )

            binding.layoutPlantsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPlants.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvPlants.adapter = PlantCareAdapter(
                activity = activity,
                list = filtered,
                onWaterClick = { record ->
                    store.waterPlant(record.id)
                    Toast.makeText(activity, "💧 已完成【${record.name}】浇水打卡！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onFertilizeClick = { record ->
                    store.fertilizePlant(record.id)
                    Toast.makeText(activity, "🌿 已完成【${record.name}】施肥打卡！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { record ->
                    showAddOrEditPlantDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出绿植",
                        message = "确认移出【${record.name}】？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deletePlantRecord(record.id)
                        Toast.makeText(activity, "已移出绿植记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvPlants.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupPlantFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_plant_water_due -> "water_due"
                R.id.chip_plant_fert_due -> "fert_due"
                R.id.chip_plant_full_sun -> "full_sun"
                R.id.chip_plant_semi_shade -> "semi_shade"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchPlants) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddPlant.applyPressScaleAnimation(0.92f)
        binding.btnAddPlant.setOnClickListener {
            showAddOrEditPlantDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnClosePlantVault.applyPressScaleAnimation(0.92f)
        binding.btnClosePlantVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出录入/编辑绿植弹窗 */
    fun showAddOrEditPlantDialog(
        activity: Activity,
        store: DataStore,
        editingRecord: PlantCareRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_plant, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_plant_title)
        val chipGroupLight = view.findViewById<ChipGroup>(R.id.chip_group_plant_light_edit)
        val etName = view.findViewById<EditText>(R.id.et_plant_name)
        val etSpecies = view.findViewById<EditText>(R.id.et_plant_species)
        val etLocation = view.findViewById<EditText>(R.id.et_plant_location)
        val etWaterDays = view.findViewById<EditText>(R.id.et_plant_water_days)
        val etFertDays = view.findViewById<EditText>(R.id.et_plant_fert_days)
        val etTips = view.findViewById<EditText>(R.id.et_plant_tips)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_plant)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_plant)

        if (editingRecord != null) {
            tvTitle.text = "🪴 编辑绿植花卉档案"
            when (editingRecord.lightDemand) {
                "full_sun" -> chipGroupLight.check(R.id.chip_edit_light_full_sun)
                "shade" -> chipGroupLight.check(R.id.chip_edit_light_shade)
                else -> chipGroupLight.check(R.id.chip_edit_light_semi_shade)
            }
            etName.setText(editingRecord.name)
            etSpecies.setText(editingRecord.species)
            etLocation.setText(editingRecord.location)
            etWaterDays.setText("${editingRecord.waterIntervalDays}")
            etFertDays.setText("${editingRecord.fertilizeIntervalDays}")
            etTips.setText(editingRecord.careTips)
        } else {
            tvTitle.text = "🪴 添入家庭绿植花卉"
            chipGroupLight.check(R.id.chip_edit_light_semi_shade)
            etWaterDays.setText("7")
            etFertDays.setText("30")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入绿植名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLight = when (chipGroupLight.checkedChipId) {
                R.id.chip_edit_light_full_sun -> "full_sun"
                R.id.chip_edit_light_shade -> "shade"
                else -> "semi_shade"
            }

            val species = etSpecies.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val waterDays = etWaterDays.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 7
            val fertDays = etFertDays.text.toString().toIntOrNull() ?: 30
            val tips = etTips.text.toString().trim()

            val record = editingRecord?.copy(
                name = name,
                species = species,
                location = location,
                lightDemand = selectedLight,
                waterIntervalDays = waterDays,
                fertilizeIntervalDays = fertDays,
                careTips = tips
            ) ?: PlantCareRecord(
                id = UUID.randomUUID().toString(),
                name = name,
                species = species,
                location = location,
                lightDemand = selectedLight,
                waterIntervalDays = waterDays,
                lastWateredAt = System.currentTimeMillis(),
                fertilizeIntervalDays = fertDays,
                lastFertilizedAt = if (fertDays > 0) System.currentTimeMillis() else 0L,
                careTips = tips
            )

            store.addOrUpdatePlantRecord(record)
            Toast.makeText(activity, "🎉 【$name】已纳入绿植水肥养护日历！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
