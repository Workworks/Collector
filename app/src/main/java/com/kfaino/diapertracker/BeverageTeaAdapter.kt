package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemBeverageRecordBinding
import java.util.Date

/**
 * 🍷 茶窖珍藏与适饮时效列表适配器 (Cellar & Tea Vault Adapter)
 */
class BeverageTeaAdapter(
    private val activity: Activity,
    private val list: List<BeverageTeaRecord>,
    private val onOpenClick: (BeverageTeaRecord) -> Unit,
    private val onConsumeQtyClick: (BeverageTeaRecord) -> Unit,
    private val onEditClick: (BeverageTeaRecord) -> Unit,
    private val onDeleteClick: (BeverageTeaRecord) -> Unit
) : RecyclerView.Adapter<BeverageTeaAdapter.BeverageViewHolder>() {

    class BeverageViewHolder(val binding: ItemBeverageRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeverageViewHolder {
        val binding = ItemBeverageRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BeverageViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: BeverageViewHolder, position: Int) {
        val record = list[position]
        val binding = holder.binding

        val catIcon = when {
            record.category.contains("茶") -> "🍵"
            record.category.contains("葡萄酒") || record.category.contains("酒庄") -> "🍷"
            record.category.contains("烈酒") || record.category.contains("白酒") || record.category.contains("威士忌") -> "🥃"
            record.category.contains("咖啡") -> "☕"
            else -> "🍾"
        }
        binding.itemBeverageCategoryBadge.text = "$catIcon ${record.category}"

        val agingYears = record.getAgingYears()
        val vintageStr = if (record.vintageYear > 0) "📅 ${record.vintageYear}年 · 陈化${agingYears}年" else "📅 经典珍藏"
        binding.itemBeverageVintageBadge.text = vintageStr

        // 状态徽章与颜色
        binding.itemBeverageStatusBadge.text = record.getStatusDisplayName()
        when {
            record.isOpenExpired() -> {
                binding.itemBeverageStatusBadge.setTextColor(activity.getColor(R.color.danger))
            }
            record.isOpened() -> {
                binding.itemBeverageStatusBadge.setTextColor(activity.getColor(R.color.accent_dark))
            }
            record.isPeakDrinkingNow() -> {
                binding.itemBeverageStatusBadge.setTextColor(activity.getColor(R.color.primary))
            }
            else -> {
                binding.itemBeverageStatusBadge.setTextColor(activity.getColor(R.color.text_secondary))
            }
        }

        val originPart = if (record.originRegion.isNotBlank()) " · ${record.originRegion}" else ""
        binding.itemBeverageNameOrigin.text = "${record.name}$originPart"

        val locStr = if (record.storageLocation.isNotBlank()) "📍 存放于: ${record.storageLocation}" else "📍 存放位置未指定"
        binding.itemBeverageLocationQty.text = "$locStr (库存: ${record.qty} ${record.unit})"

        // 开瓶保鲜信息
        if (record.isOpened()) {
            binding.itemBeverageOpenedInfo.visibility = View.VISIBLE
            val openDateStr = VaultUiHelper.standardDateFormat.format(Date(record.openedAt))
            val lifeStr = if (record.openShelfLifeDays > 0) " · 保质/赏味期: ${record.openShelfLifeDays}天" else ""
            val warningStr = if (record.isOpenExpired()) " (⚠️ 已超期)" else ""
            binding.itemBeverageOpenedInfo.text = "🍷 已于 $openDateStr 开瓶品鉴$lifeStr$warningStr"
            binding.btnItemOpenBeverage.visibility = View.GONE
        } else {
            binding.itemBeverageOpenedInfo.visibility = View.GONE
            binding.btnItemOpenBeverage.visibility = View.VISIBLE
        }

        // 风味特征与冲泡笔记
        if (record.tastingNotes.isNotBlank()) {
            binding.itemBeverageNotes.visibility = View.VISIBLE
            binding.itemBeverageNotes.text = "📝 冲泡/风味: ${record.tastingNotes}"
        } else {
            binding.itemBeverageNotes.visibility = View.GONE
        }

        // 按钮交互动效与监听
        binding.btnItemOpenBeverage.applyPressScaleAnimation(0.92f)
        binding.btnItemOpenBeverage.setOnClickListener { onOpenClick(record) }

        binding.btnItemConsumeQty.applyPressScaleAnimation(0.92f)
        binding.btnItemConsumeQty.setOnClickListener { onConsumeQtyClick(record) }
        binding.btnItemConsumeQty.visibility = if (record.qty > 0) View.VISIBLE else View.GONE

        binding.btnItemEditBeverage.applyPressScaleAnimation(0.90f)
        binding.btnItemEditBeverage.setOnClickListener { onEditClick(record) }

        binding.btnItemDeleteBeverage.applyPressScaleAnimation(0.90f)
        binding.btnItemDeleteBeverage.setOnClickListener { onDeleteClick(record) }
    }
}
