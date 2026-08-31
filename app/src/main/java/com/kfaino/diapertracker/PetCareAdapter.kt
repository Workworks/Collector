package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemPetRecordBinding
import java.util.Date

/**
 * 🐾 家庭萌宠档案与健康耗材列表适配器 (Pet Care Adapter)
 */
class PetCareAdapter(
    private val activity: Activity,
    private val list: List<PetCareRecord>,
    private val onDewormClick: (PetCareRecord) -> Unit,
    private val onVaccineClick: (PetCareRecord) -> Unit,
    private val onEditClick: (PetCareRecord) -> Unit,
    private val onDeleteClick: (PetCareRecord) -> Unit
) : RecyclerView.Adapter<PetCareAdapter.PetViewHolder>() {

    class PetViewHolder(val binding: ItemPetRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val record = list[position]
        val binding = holder.binding

        val speciesIcon = when {
            record.species.contains("狗") || record.species.contains("犬") -> "🐶"
            record.species.contains("猫") -> "🐱"
            record.species.contains("鸟") || record.species.contains("鹦鹉") -> "🦜"
            else -> "🐾"
        }
        binding.itemPetSpeciesBadge.text = "$speciesIcon ${record.species}"

        if (record.weightKg > 0.0) {
            binding.itemPetWeightBadge.visibility = View.VISIBLE
            binding.itemPetWeightBadge.text = "⚖️ ${record.weightKg} kg"
        } else {
            binding.itemPetWeightBadge.visibility = View.GONE
        }

        binding.itemPetName.text = record.name

        // 驱虫与疫苗状态角标
        if (record.isDewormDue()) {
            binding.itemPetDewormStatusBadge.visibility = View.VISIBLE
            binding.itemPetDewormStatusBadge.text = "💊 需驱虫"
        } else if (record.isDewormDueSoon()) {
            binding.itemPetDewormStatusBadge.visibility = View.VISIBLE
            binding.itemPetDewormStatusBadge.text = "⏳ 7天内待驱虫"
        } else {
            binding.itemPetDewormStatusBadge.visibility = View.GONE
        }

        if (record.isVaccineDue()) {
            binding.itemPetVaxStatusBadge.visibility = View.VISIBLE
            binding.itemPetVaxStatusBadge.text = "💉 需打疫苗"
        } else if (record.isVaccineDueSoon()) {
            binding.itemPetVaxStatusBadge.visibility = View.VISIBLE
            binding.itemPetVaxStatusBadge.text = "⏳ 30天内待接种"
        } else {
            binding.itemPetVaxStatusBadge.visibility = View.GONE
        }

        // 芯片号 (脱敏与一键复制)
        if (record.microchipId.isNotBlank()) {
            binding.layoutPetChip.visibility = View.VISIBLE
            val maskedChip = if (record.microchipId.length > 8) {
                "${record.microchipId.take(4)}****${record.microchipId.takeLast(4)}"
            } else {
                record.microchipId
            }
            binding.itemPetChipText.text = "🆔 芯片/证号: $maskedChip (点击复制)"
            binding.layoutPetChip.setOnClickListener {
                VaultUiHelper.copyToClipboard(activity, "萌宠芯片号", record.microchipId)
                Toast.makeText(activity, "📋 芯片号已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.layoutPetChip.visibility = View.GONE
        }

        // 驱虫与疫苗排期
        val dewormInfo = if (record.dewormIntervalDays > 0) {
            val nextDeworm = record.getNextDewormDate()
            val nextDewormStr = if (nextDeworm > 0L) VaultUiHelper.standardDateFormat.format(Date(nextDeworm)) else "待打卡"
            "💊 驱虫: 每 ${record.dewormIntervalDays} 天 (下次: $nextDewormStr)"
        } else {
            "💊 驱虫: 无需定期"
        }

        val vaxInfo = if (record.vaccineIntervalDays > 0) {
            val nextVax = record.getNextVaccineDate()
            val nextVaxStr = if (nextVax > 0L) VaultUiHelper.standardDateFormat.format(Date(nextVax)) else "待打卡"
            " · 💉 疫苗: (下次: $nextVaxStr)"
        } else {
            ""
        }
        binding.itemPetScheduleText.text = "$dewormInfo$vaxInfo"

        // 主粮品牌与位置
        if (record.foodBrand.isNotBlank()) {
            binding.itemPetFoodText.visibility = View.VISIBLE
            binding.itemPetFoodText.text = "🥣 主粮: ${record.foodBrand}"
        } else {
            binding.itemPetFoodText.visibility = View.GONE
        }

        // 健康禁忌与病历备忘
        if (record.notes.isNotBlank()) {
            binding.itemPetNotes.visibility = View.VISIBLE
            binding.itemPetNotes.text = "📝 备忘: ${record.notes}"
        } else {
            binding.itemPetNotes.visibility = View.GONE
        }

        // 交互与动效
        binding.btnItemDewormPet.applyPressScaleAnimation(0.92f)
        binding.btnItemDewormPet.setOnClickListener { onDewormClick(record) }
        binding.btnItemDewormPet.visibility = if (record.dewormIntervalDays > 0) View.VISIBLE else View.GONE

        binding.btnItemVaxPet.applyPressScaleAnimation(0.92f)
        binding.btnItemVaxPet.setOnClickListener { onVaccineClick(record) }
        binding.btnItemVaxPet.visibility = if (record.vaccineIntervalDays > 0) View.VISIBLE else View.GONE

        binding.btnItemEditPet.applyPressScaleAnimation(0.90f)
        binding.btnItemEditPet.setOnClickListener { onEditClick(record) }

        binding.btnItemDeletePet.applyPressScaleAnimation(0.90f)
        binding.btnItemDeletePet.setOnClickListener { onDeleteClick(record) }
    }
}
