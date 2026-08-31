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
import com.kfaino.diapertracker.databinding.DialogPetVaultBinding
import java.util.UUID

/**
 * 🐾 家庭萌宠档案与健康耗材控制器 (Pet Care Vault Dialog)
 * - 支持宠物物种/品种档案、体重记录与芯片号脱敏速查
 * - 支持驱虫与疫苗排期倒计时计算、临期/逾期强提醒
 * - 一键驱虫/疫苗接种打卡
 */
object PetCareDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogPetVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentFilter = "all" // "all", "deworm_due", "vax_due"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allRecords = store.getPetRecords()
            val dewormDueCount = allRecords.count { it.isDewormDue() }
            val vaxDueCount = allRecords.count { it.isVaccineDue() }

            binding.tvStatTotalPets.text = "${allRecords.size} 只"
            binding.tvStatDewormDuePets.text = "$dewormDueCount 只"
            binding.tvStatVaxDuePets.text = "$vaxDueCount 只"

            val filtered = allRecords.filter { record ->
                val matchesFilter = when (currentFilter) {
                    "deworm_due" -> record.isDewormDue()
                    "vax_due" -> record.isVaccineDue()
                    else -> true
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        record.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.species.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.microchipId.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.foodBrand.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesFilter && matchesSearch
            }.sortedWith(
                compareByDescending<PetCareRecord> { it.isVaccineDue() }
                    .thenByDescending { it.isDewormDue() }
            )

            binding.layoutPetsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPets.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvPets.adapter = PetCareAdapter(
                activity = activity,
                list = filtered,
                onDewormClick = { record ->
                    store.markPetDewormed(record.id)
                    Toast.makeText(activity, "💊 已完成【${record.name}】驱虫打卡！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onVaccineClick = { record ->
                    store.markPetVaccinated(record.id)
                    Toast.makeText(activity, "💉 已完成【${record.name}】疫苗接种打卡！", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onEditClick = { record ->
                    showAddOrEditPetDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出萌宠档案",
                        message = "确认移出【${record.name}】的档案记录？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deletePetRecord(record.id)
                        Toast.makeText(activity, "已移出萌宠档案记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvPets.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupPetFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_pet_deworm_due -> "deworm_due"
                R.id.chip_pet_vax_due -> "vax_due"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchPets) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddPet.applyPressScaleAnimation(0.92f)
        binding.btnAddPet.setOnClickListener {
            showAddOrEditPetDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnClosePetVault.applyPressScaleAnimation(0.92f)
        binding.btnClosePetVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出登记/编辑萌宠档案弹窗 */
    fun showAddOrEditPetDialog(
        activity: Activity,
        store: DataStore,
        editingRecord: PetCareRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_pet, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_pet_title)
        val chipGroupSpecies = view.findViewById<ChipGroup>(R.id.chip_group_pet_species_edit)
        val etName = view.findViewById<EditText>(R.id.et_pet_name)
        val etSpeciesDetail = view.findViewById<EditText>(R.id.et_pet_species_detail)
        val etWeight = view.findViewById<EditText>(R.id.et_pet_weight)
        val etChipId = view.findViewById<EditText>(R.id.et_pet_chip_id)
        val etDewormDays = view.findViewById<EditText>(R.id.et_pet_deworm_days)
        val etVaxDays = view.findViewById<EditText>(R.id.et_pet_vax_days)
        val etFood = view.findViewById<EditText>(R.id.et_pet_food)
        val etNotes = view.findViewById<EditText>(R.id.et_pet_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_pet)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_pet)

        if (editingRecord != null) {
            tvTitle.text = "🐾 编辑萌宠档案"
            when {
                editingRecord.species.contains("狗") || editingRecord.species.contains("犬") ->
                    chipGroupSpecies.check(R.id.chip_edit_species_dog)
                editingRecord.species.contains("鸟") || editingRecord.species.contains("鹦鹉") ->
                    chipGroupSpecies.check(R.id.chip_edit_species_bird)
                editingRecord.species.contains("猫") ->
                    chipGroupSpecies.check(R.id.chip_edit_species_cat)
                else -> chipGroupSpecies.check(R.id.chip_edit_species_other)
            }
            etName.setText(editingRecord.name)
            etSpeciesDetail.setText(editingRecord.species)
            if (editingRecord.weightKg > 0.0) etWeight.setText("${editingRecord.weightKg}")
            etChipId.setText(editingRecord.microchipId)
            etDewormDays.setText("${editingRecord.dewormIntervalDays}")
            etVaxDays.setText("${editingRecord.vaccineIntervalDays}")
            etFood.setText(editingRecord.foodBrand)
            etNotes.setText(editingRecord.notes)
        } else {
            tvTitle.text = "🐾 登记家庭萌宠档案"
            chipGroupSpecies.check(R.id.chip_edit_species_cat)
            etSpeciesDetail.setText("猫咪")
            etDewormDays.setText("30")
            etVaxDays.setText("365")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入萌宠昵称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val species = etSpeciesDetail.text.toString().trim().ifBlank {
                when (chipGroupSpecies.checkedChipId) {
                    R.id.chip_edit_species_dog -> "狗狗"
                    R.id.chip_edit_species_bird -> "鸟类"
                    R.id.chip_edit_species_other -> "其它"
                    else -> "猫咪"
                }
            }

            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val chipId = etChipId.text.toString().trim()
            val dewormDays = etDewormDays.text.toString().toIntOrNull() ?: 30
            val vaxDays = etVaxDays.text.toString().toIntOrNull() ?: 365
            val food = etFood.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            val record = editingRecord?.copy(
                name = name,
                species = species,
                weightKg = weight,
                microchipId = chipId,
                dewormIntervalDays = dewormDays,
                vaccineIntervalDays = vaxDays,
                foodBrand = food,
                notes = notes
            ) ?: PetCareRecord(
                id = UUID.randomUUID().toString(),
                name = name,
                species = species,
                birthDate = System.currentTimeMillis(),
                weightKg = weight,
                microchipId = chipId,
                dewormIntervalDays = dewormDays,
                lastDewormedAt = System.currentTimeMillis(),
                vaccineIntervalDays = vaxDays,
                lastVaccinatedAt = if (vaxDays > 0) System.currentTimeMillis() else 0L,
                foodBrand = food,
                notes = notes
            )

            store.addOrUpdatePetRecord(record)
            Toast.makeText(activity, "🎉 【$name】已成功纳入萌宠健康档案！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
