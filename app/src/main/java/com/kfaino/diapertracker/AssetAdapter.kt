package com.kfaino.diapertracker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemAssetCardBinding
import java.util.Locale

class AssetAdapter(
    private val onEntryClick: (Entry) -> Unit,
    private val onMoreClick: (Entry, View) -> Unit
) : RecyclerView.Adapter<AssetAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    fun submitList(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAssetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        val b = holder.binding

        holder.itemView.applyPressScaleAnimation(0.96f)
        holder.itemView.setOnClickListener { onEntryClick(entry) }
        b.btnItemMore.setOnClickListener { onMoreClick(entry, it) }

        // 1. 图标/首字徽章
        val emoji = getCategoryEmoji(entry.category)
        b.itemIconBadge.text = emoji

        // 2. 物品名称与星标
        b.itemName.text = entry.brand
        b.itemImportantTag.visibility = if (entry.isImportant) View.VISIBLE else View.GONE

        // 3. 退役状态与归置标签
        if (entry.isRetired) {
            b.itemRetiredBadge.visibility = View.VISIBLE
            val actionText = if (entry.retiredAction.isNotBlank()) entry.retiredAction else "已退役"
            b.itemRetiredBadge.text = "🔴 $actionText"
            b.itemDaysOwned.setTextColor(Color.parseColor("#94A3B8"))
        } else {
            b.itemRetiredBadge.visibility = View.GONE
            b.itemDaysOwned.setTextColor(Color.parseColor("#F1F5F9"))
        }

        // 4. 拥有天数
        b.itemDaysOwned.text = "${entry.getDaysOwned()}"

        // 5. 价格与日均消费
        val totalVal = entry.price * entry.qty
        b.itemOriginalPrice.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalVal)}"

        val dailyCost = entry.getDailyCost()
        b.itemDailyCost.text = "¥${String.format(Locale.getDefault(), "%.2f", dailyCost)}/天"

        // 6. 空间位置标签
        if (entry.location.isNotBlank()) {
            b.itemLocationTag.visibility = View.VISIBLE
            b.itemLocationTag.text = "📍 ${entry.location}"
        } else {
            b.itemLocationTag.visibility = View.GONE
        }

        // 7. 在库数量
        b.itemStockBadge.text = "${entry.qty} ${entry.unit}"
    }

    private fun getCategoryEmoji(cat: String): String {
        return when {
            cat.contains("数码") || cat.contains("电脑") || cat.contains("手机") -> "💻"
            cat.contains("日用") || cat.contains("家电") -> "🧴"
            cat.contains("零食") || cat.contains("食品") -> "🍪"
            cat.contains("耗材") || cat.contains("办公") -> "📦"
            cat.contains("贵重") || cat.contains("证件") -> "🔑"
            cat.contains("服饰") || cat.contains("穿搭") -> "👔"
            else -> "📦"
        }
    }

    class VH(val binding: ItemAssetCardBinding) : RecyclerView.ViewHolder(binding.root)
}
