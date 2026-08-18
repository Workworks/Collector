package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemBrandSummaryBinding
import com.kfaino.diapertracker.databinding.ItemCategoryHeaderBinding
import java.util.Locale

/**
 * 分组 RecyclerView 适配器
 * 两种 viewType: HEADER = 分类标题, BRAND = 分类下的品牌卡片
 */
class CategoryAdapter(
    private val onBrandClick: ((BrandSummary, String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_BRAND = 1
    }

    sealed class ListItem {
        data class Header(val category: String, val totalCount: Int, val totalAmount: Double, val unit: String = "片") : ListItem()
        data class Brand(val brand: BrandSummary, val category: String) : ListItem()
    }

    private var items: List<ListItem> = emptyList()
    private val maxPerCategory = HashMap<String, Int>()

    fun submit(groups: List<CategoryGroup>) {
        val flat = mutableListOf<ListItem>()
        maxPerCategory.clear()
        for (g in groups) {
            flat.add(ListItem.Header(g.name, g.totalCount, g.totalAmount, g.unit))
            val maxCount = g.brands.maxOfOrNull { it.count } ?: 0
            maxPerCategory[g.name] = maxCount.coerceAtLeast(1)
            for (b in g.brands) {
                flat.add(ListItem.Brand(b, g.name))
            }
        }
        items = flat
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ListItem.Header -> TYPE_HEADER
        is ListItem.Brand -> TYPE_BRAND
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemCategoryHeaderBinding.inflate(inflater, parent, false))
            else -> BrandVH(ItemBrandSummaryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ctx = holder.itemView.context
        when (val item = items[position]) {
            is ListItem.Header -> {
                val vh = holder as HeaderVH
                vh.binding.catName.text = item.category
                vh.binding.catTotal.text = "共 ${item.totalCount} ${item.unit} · ¥${String.format(Locale.getDefault(), "%.2f", item.totalAmount)}"
            }
            is ListItem.Brand -> {
                val vh = holder as BrandVH
                val brand = item.brand
                val b = vh.binding
                val u = brand.unit.ifEmpty { "片" }

                b.brandName.text = (if (brand.isImportant) "⭐ " else "") + brand.name

                // 空间与位置信息
                if (brand.location.isNotBlank()) {
                    b.itemLocationTag.visibility = View.VISIBLE
                    b.itemLocationTag.text = "📍 ${brand.houseName} · ${brand.location}"
                } else {
                    b.itemLocationTag.visibility = View.GONE
                }

                b.itemAvgPrice.text = if (brand.avgPrice > 0)
                    "均价 ¥${String.format(Locale.getDefault(), "%.2f", brand.avgPrice)}/$u"
                else
                    "未设单价"

                b.itemAmount.text = "累计 ¥${String.format(Locale.getDefault(), "%.2f", brand.amount)}"

                b.avatar.text = brand.name.take(1)
                b.avatar.backgroundTintList = ColorStateList.valueOf(colorFor(brand.name))

                // 库存状态与标签
                when {
                    brand.count > 10 -> {
                        b.stockBadgeLayout.setBackgroundResource(R.drawable.bg_stock_healthy)
                        b.itemCount.setTextColor(ContextCompat.getColor(ctx, R.color.stock_healthy_text))
                        b.itemCount.text = "${brand.count} $u"
                    }
                    brand.count in 1..10 -> {
                        b.stockBadgeLayout.setBackgroundResource(R.drawable.bg_stock_low)
                        b.itemCount.setTextColor(ContextCompat.getColor(ctx, R.color.stock_low_text))
                        b.itemCount.text = "${brand.count} $u (紧张)"
                    }
                    else -> {
                        b.stockBadgeLayout.setBackgroundResource(R.drawable.bg_stock_empty)
                        b.itemCount.setTextColor(ContextCompat.getColor(ctx, R.color.stock_empty_text))
                        b.itemCount.text = "${brand.count} $u (缺货)"
                    }
                }

                // 占比进度条
                val max = maxPerCategory[item.category] ?: 1
                val progress = if (max > 0) (brand.count.coerceAtLeast(0) * 100 / max).coerceIn(0, 100) else 0
                b.shareBar.progress = progress

                // 动效与交互
                holder.itemView.applyPressScaleAnimation(0.96f)
                holder.itemView.setOnClickListener {
                    onBrandClick?.invoke(brand, item.category)
                }
            }
        }
    }

    private fun colorFor(name: String): Int {
        val colors = intArrayOf(
            Color.parseColor("#10B981"), Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#F59E0B"),
            Color.parseColor("#EC4899"), Color.parseColor("#06B6D4"),
            Color.parseColor("#F97316"), Color.parseColor("#6366F1")
        )
        val hash = Math.abs(name.hashCode())
        return colors[hash % colors.size]
    }

    class HeaderVH(val binding: ItemCategoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class BrandVH(val binding: ItemBrandSummaryBinding) : RecyclerView.ViewHolder(binding.root)
}
