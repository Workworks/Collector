package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemPlantRecordBinding
import java.util.Date

/**
 * 🪴 家庭绿植花卉与水肥养护列表适配器 (Plant Care Adapter)
 */
class PlantCareAdapter(
    private val activity: Activity,
    private val list: List<PlantCareRecord>,
    private val onWaterClick: (PlantCareRecord) -> Unit,
    private val onFertilizeClick: (PlantCareRecord) -> Unit,
    private val onEditClick: (PlantCareRecord) -> Unit,
    private val onDeleteClick: (PlantCareRecord) -> Unit
) : RecyclerView.Adapter<PlantCareAdapter.PlantViewHolder>() {

    class PlantViewHolder(val binding: ItemPlantRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val record = list[position]
        val binding = holder.binding

        binding.itemPlantName.text = record.name
        binding.itemPlantLightBadge.text = record.getLightDemandDisplayName()
        
        if (record.species.isNotBlank()) {
            binding.itemPlantSpeciesBadge.visibility = View.VISIBLE
            binding.itemPlantSpeciesBadge.text = record.species
        } else {
            binding.itemPlantSpeciesBadge.visibility = View.GONE
        }

        // 浇水与施肥状态角标
        if (record.isWaterDue()) {
            binding.itemPlantWaterStatusBadge.visibility = View.VISIBLE
            binding.itemPlantWaterStatusBadge.text = "💧 需浇水"
        } else if (record.isWaterDueSoon()) {
            binding.itemPlantWaterStatusBadge.visibility = View.VISIBLE
            binding.itemPlantWaterStatusBadge.text = "⏳ 今日/明日待浇"
        } else {
            binding.itemPlantWaterStatusBadge.visibility = View.GONE
        }

        if (record.isFertilizeDue()) {
            binding.itemPlantFertStatusBadge.visibility = View.VISIBLE
            binding.itemPlantFertStatusBadge.text = "🌿 需施肥"
        } else {
            binding.itemPlantFertStatusBadge.visibility = View.GONE
        }

        // 摆放位置
        val locStr = if (record.location.isNotBlank()) record.location else "未标注摆放位"
        binding.itemPlantLocation.text = "📍 摆放位置: $locStr"

        // 浇水与施肥排期
        val nextWater = record.getNextWaterDate()
        val nextWaterStr = if (nextWater > 0L) VaultUiHelper.standardDateFormat.format(Date(nextWater)) else "待打卡"
        val waterInfo = "💧 浇水: 每 ${record.waterIntervalDays} 天 (下次: $nextWaterStr)"

        val fertInfo = if (record.fertilizeIntervalDays > 0) {
            val nextFert = record.getNextFertilizeDate()
            val nextFertStr = if (nextFert > 0L) VaultUiHelper.standardDateFormat.format(Date(nextFert)) else "待打卡"
            " · 🌿 施肥: 每 ${record.fertilizeIntervalDays} 天 (下次: $nextFertStr)"
        } else {
            ""
        }
        binding.itemPlantScheduleText.text = "$waterInfo$fertInfo"

        // 养护要领
        if (record.careTips.isNotBlank()) {
            binding.itemPlantTips.visibility = View.VISIBLE
            binding.itemPlantTips.text = "📝 养护: ${record.careTips}"
        } else {
            binding.itemPlantTips.visibility = View.GONE
        }

        // 交互与动效
        binding.btnItemWaterPlant.applyPressScaleAnimation(0.92f)
        binding.btnItemWaterPlant.setOnClickListener { onWaterClick(record) }

        binding.btnItemFertPlant.applyPressScaleAnimation(0.92f)
        binding.btnItemFertPlant.setOnClickListener { onFertilizeClick(record) }
        binding.btnItemFertPlant.visibility = if (record.fertilizeIntervalDays > 0) View.VISIBLE else View.GONE

        binding.btnItemEditPlant.applyPressScaleAnimation(0.90f)
        binding.btnItemEditPlant.setOnClickListener { onEditClick(record) }

        binding.btnItemDeletePlant.applyPressScaleAnimation(0.90f)
        binding.btnItemDeletePlant.setOnClickListener { onDeleteClick(record) }
    }
}
