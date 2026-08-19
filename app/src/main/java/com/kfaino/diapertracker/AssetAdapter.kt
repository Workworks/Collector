package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemAssetCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssetAdapter(
    private val onEntryClick: (Entry) -> Unit,
    private val onMoreClick: (Entry, View) -> Unit,
    private val onLocationClick: ((Entry) -> Unit)? = null,
    private val onPhotoClick: ((Entry) -> Unit)? = null,
    private val onReceiptClick: ((Entry) -> Unit)? = null
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
        val ctx = holder.itemView.context

        holder.itemView.applyPressScaleAnimation(0.96f)
        holder.itemView.setOnClickListener { onEntryClick(entry) }
        b.btnItemMore.setOnClickListener { onMoreClick(entry, it) }

        // 1. 图标/实物缩略图
        val emoji = getCategoryEmoji(entry.category)
        b.itemIconBadge.text = emoji

        if (entry.photoPath.isNotBlank()) {
            val thumb = ImageVaultHelper.loadSampledBitmap(ctx, entry.photoPath, 120, 120)
            if (thumb != null) {
                b.itemPhotoThumbnail.visibility = View.VISIBLE
                b.itemPhotoThumbnail.setImageBitmap(thumb)
                b.itemPhotoThumbnail.setOnClickListener {
                    onPhotoClick?.invoke(entry) ?: run {
                        (ctx as? Activity)?.let { act ->
                            PhotoPreviewDialog.show(act, "${entry.brand} · 实物照片", entry.photoPath)
                        }
                    }
                }
            } else {
                b.itemPhotoThumbnail.visibility = View.GONE
            }
        } else {
            b.itemPhotoThumbnail.visibility = View.GONE
        }

        // 2. 物品名称与星标
        b.itemName.text = entry.brand
        b.itemImportantTag.visibility = if (entry.isImportant) View.VISIBLE else View.GONE

        // 3. 退役状态与归置标签
        if (entry.isRetired) {
            b.itemRetiredBadge.visibility = View.VISIBLE
            val actionText = if (entry.retiredAction.isNotBlank()) entry.retiredAction else "已退役"
            b.itemRetiredBadge.text = "🔴 $actionText"
            b.itemDaysOwned.setTextColor(ContextCompat.getColor(ctx, R.color.text_tertiary))
        } else {
            b.itemRetiredBadge.visibility = View.GONE
            b.itemDaysOwned.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        // 4. 按物品管理类型（折旧 / 保质期 / 长期耐用 / 消耗品）差异化展示指标
        val totalVal = entry.price * entry.qty
        b.itemOriginalPrice.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalVal)}"

        when (entry.assetType) {
            "expiring" -> {
                // 保质期物品：显示到期倒计时与到期日期，不显示折旧费
                if (entry.expiryDate > 0) {
                    val statusText = entry.getExpiryStatusText()
                    b.itemDaysOwned.text = statusText
                    b.itemDaysLabel.text = ""
                    val expDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.expiryDate))
                    b.itemDailyCost.text = "到期: $expDateStr"
                } else {
                    b.itemDaysOwned.text = "保质期"
                    b.itemDaysLabel.text = ""
                    b.itemDailyCost.text = "未设到期日"
                }
            }
            "durable" -> {
                // 长期耐用品：显示拥有天数，不计折旧
                b.itemDaysOwned.text = "${entry.getDaysOwned()}"
                b.itemDaysLabel.text = " 天"
                b.itemDailyCost.text = "🛋️ 长期耐用品"
            }
            "consumable" -> {
                // 日常消耗品：显示库存数量
                b.itemDaysOwned.text = "${entry.qty}"
                b.itemDaysLabel.text = " ${entry.unit}"
                b.itemDailyCost.text = "📦 日常耗材"
            }
            else -> {
                // 折旧资产 (depreciating)：显示拥有天数与日均折旧成本
                b.itemDaysOwned.text = "${entry.getDaysOwned()}"
                b.itemDaysLabel.text = " 天"
                val dailyCost = entry.getDailyCost()
                b.itemDailyCost.text = "¥${String.format(Locale.getDefault(), "%.2f", dailyCost)}/天"
            }
        }

        // 5. 发票凭证快捷徽章
        if (entry.receiptPath.isNotBlank()) {
            b.itemReceiptBadge.visibility = View.VISIBLE
            b.itemReceiptBadge.applyPressScaleAnimation(0.92f)
            b.itemReceiptBadge.setOnClickListener {
                onReceiptClick?.invoke(entry) ?: run {
                    (ctx as? Activity)?.let { act ->
                        PhotoPreviewDialog.show(act, "${entry.brand} · 购买发票/保修卡凭证", entry.receiptPath)
                    }
                }
            }
        } else {
            b.itemReceiptBadge.visibility = View.GONE
        }

        // 6. 空间位置标签 (点击可穿梭至平面图)
        if (entry.location.isNotBlank()) {
            b.itemLocationTag.visibility = View.VISIBLE
            b.itemLocationTag.text = "📍 ${entry.location}"
            b.itemLocationTag.applyPressScaleAnimation(0.92f)
            b.itemLocationTag.setOnClickListener {
                onLocationClick?.invoke(entry)
            }
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
