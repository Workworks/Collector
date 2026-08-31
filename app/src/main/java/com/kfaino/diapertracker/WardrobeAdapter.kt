package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemWardrobeRecordBinding
import java.util.Locale

/**
 * 👗 换季衣橱与四季穿搭列表适配器 (Wardrobe Adapter)
 */
class WardrobeAdapter(
    private val activity: Activity,
    private val list: List<WardrobeRecord>,
    private val onWearClick: (WardrobeRecord) -> Unit,
    private val onToggleSealClick: (WardrobeRecord) -> Unit,
    private val onEditClick: (WardrobeRecord) -> Unit,
    private val onDeleteClick: (WardrobeRecord) -> Unit
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    class WardrobeViewHolder(val binding: ItemWardrobeRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val binding = ItemWardrobeRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WardrobeViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = list[position]
        val binding = holder.binding

        binding.itemWardrobeName.text = item.name
        binding.itemWardrobeSeasonBadge.text = item.getSeasonDisplayName()
        binding.itemWardrobeCategoryBadge.text = item.getCategoryDisplayName()

        // 封箱状态
        if (item.isSealed) {
            binding.itemWardrobeSealedBadge.visibility = View.VISIBLE
            binding.btnItemToggleSeal.text = "🔓 解封入柜"
        } else {
            binding.itemWardrobeSealedBadge.visibility = View.GONE
            binding.btnItemToggleSeal.text = "📦 换季封箱"
        }

        // 沉睡预警
        if (item.isSleeping()) {
            binding.itemWardrobeSleepingWarning.visibility = View.VISIBLE
        } else {
            binding.itemWardrobeSleepingWarning.visibility = View.GONE
        }

        // 面料与颜色
        val colorStr = if (item.color.isNotBlank()) item.color else "经典色"
        val matStr = if (item.material.isNotBlank()) " · ${item.material}" else ""
        binding.itemWardrobeMaterial.text = "🎨 色系/材质: $colorStr$matStr"

        // 位置
        val locStr = if (item.storageLocation.isNotBlank()) item.storageLocation else "未指定具体收纳位"
        binding.itemWardrobeLocation.text = "📍 收纳位置: $locStr"

        // 洗护
        if (item.careNotes.isNotBlank()) {
            binding.itemWardrobeCare.visibility = View.VISIBLE
            binding.itemWardrobeCare.text = "🧺 洗护: ${item.careNotes}"
        } else {
            binding.itemWardrobeCare.visibility = View.GONE
        }

        // 成本统计
        val priceStr = String.format(Locale.getDefault(), "%.2f", item.purchasePrice)
        val costPerWearStr = String.format(Locale.getDefault(), "%.2f", item.getCostPerWear())
        binding.itemWardrobeCostStats.text = "💰 购入: ¥$priceStr · 穿着 ${item.wearCount} 次 · 每次 ¥$costPerWearStr"

        // 动效与点击
        binding.btnItemWearCheckin.applyPressScaleAnimation(0.92f)
        binding.btnItemWearCheckin.setOnClickListener { onWearClick(item) }

        binding.btnItemToggleSeal.applyPressScaleAnimation(0.92f)
        binding.btnItemToggleSeal.setOnClickListener { onToggleSealClick(item) }

        binding.btnItemEditWardrobe.applyPressScaleAnimation(0.90f)
        binding.btnItemEditWardrobe.setOnClickListener { onEditClick(item) }

        binding.btnItemDeleteWardrobe.applyPressScaleAnimation(0.90f)
        binding.btnItemDeleteWardrobe.setOnClickListener { onDeleteClick(item) }
    }
}
