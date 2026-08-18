package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
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

                b.brandName.text = brand.name
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
                        b.itemCount.text = "缺货 (${brand.count} $u)"
                    }
                }

                // 进度条
                val max = maxPerCategory[item.category] ?: 1
                val progressVal = (brand.count * 100 / max).coerceIn(0, 100)
                b.shareBar.progress = progressVal

                vh.itemView.setOnClickListener { onBrandClick?.invoke(brand, item.category) }
            }
        }
    }

    private fun colorFor(name: String): Int {
        val palette = intArrayOf(
            Color.parseColor("#059669"),
            Color.parseColor("#2563EB"),
            Color.parseColor("#7C3AED"),
            Color.parseColor("#DB2777"),
            Color.parseColor("#EA580C"),
            Color.parseColor("#0D9488"),
            Color.parseColor("#4F46E5"),
            Color.parseColor("#65A30D"),
            Color.parseColor("#DC2626"),
            Color.parseColor("#0284C7"),
            Color.parseColor("#D97706"),
            Color.parseColor("#475569")
        )
        return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    class HeaderVH(val binding: ItemCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class BrandVH(val binding: ItemBrandSummaryBinding) :
        RecyclerView.ViewHolder(binding.root)
}
