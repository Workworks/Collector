package com.kfaino.diapertracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemBrandSummaryBinding
import com.kfaino.diapertracker.databinding.ItemCategoryHeaderBinding

/**
 * 分组 RecyclerView 适配器
 * 两种 viewType: HEADER = 分类标题, BRAND = 分类下的品牌卡片
 */
class CategoryAdapter(
    private val onBrandClick: ((BrandSummary) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_BRAND = 1
    }

    /** 展平后的列表项：Header 或 Brand */
    sealed class ListItem {
        data class Header(val category: String, val totalCount: Int, val totalAmount: Double) : ListItem()
        data class Brand(val brand: BrandSummary, val category: String) : ListItem()
    }

    private var items: List<ListItem> = emptyList()

    /** 每个分类下品牌数量的最大值，用于进度条归一化 */
    private val maxPerCategory = HashMap<String, Int>()

    fun submit(groups: List<CategoryGroup>) {
        val flat = mutableListOf<ListItem>()
        maxPerCategory.clear()
        for (g in groups) {
            flat.add(ListItem.Header(g.name, g.totalCount, g.totalAmount))
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
        when (val item = items[position]) {
            is ListItem.Header -> {
                val vh = holder as HeaderVH
                vh.binding.catName.text = item.category
                vh.binding.catTotal.text = "${item.totalCount}件 · ¥${String.format("%.2f", item.totalAmount)}"
            }
            is ListItem.Brand -> {
                val vh = holder as BrandVH
                val brand = item.brand
                vh.binding.brandName.text = brand.name
                vh.binding.itemCount.text = "${brand.count}件"
                vh.binding.itemAmount.text = "¥${String.format("%.2f", brand.amount)}"
                vh.binding.avatar.text = brand.name.take(1)
                vh.binding.avatar.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(colorFor(brand.name))

                // 进度条（品牌数量在该分类中的占比）
                val max = maxPerCategory[item.category] ?: 1
                vh.binding.shareBar.progress = (brand.count * 100 / max).coerceIn(0, 100)

                vh.itemView.setOnClickListener { onBrandClick?.invoke(brand) }
            }
        }
    }

    /** 根据品牌名返回稳定的颜色 */
    private fun colorFor(name: String): Int {
        val palette = intArrayOf(
            Color.parseColor("#2E7D32"),
            Color.parseColor("#1565C0"),
            Color.parseColor("#AD1457"),
            Color.parseColor("#6A1B9A"),
            Color.parseColor("#E65100"),
            Color.parseColor("#00838F"),
            Color.parseColor("#283593"),
            Color.parseColor("#AFB42B"),
            Color.parseColor("#C62828"),
            Color.parseColor("#4527A0"),
            Color.parseColor("#00695C"),
            Color.parseColor("#5D4037")
        )
        return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    class HeaderVH(val binding: ItemCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class BrandVH(val binding: ItemBrandSummaryBinding) :
        RecyclerView.ViewHolder(binding.root)
}
