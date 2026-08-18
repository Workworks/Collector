package com.kfaino.diapertracker

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.FragmentHomeBinding
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private val entries = mutableListOf<Entry>()
    private lateinit var adapter: CategoryAdapter

    // 当前筛选与排序状态
    private var selectedCategory = "全部"
    private var sortMode = 0 // 0=按分组排序, 1=按库存降序, 2=按库存升序, 3=按花费降序, 4=按名称

    companion object {
        val SORT_LABELS = arrayOf(
            "⚡ 默认分组排序",
            "📉 按库存从高到低",
            "📈 按库存从低到高",
            "💰 按花费金额从多到少",
            "🔤 按名称排序"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CategoryAdapter(
            onBrandClick = { brand, category ->
                showBrandActionSheet(brand, category)
            }
        )
        binding.categoryList.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryList.adapter = adapter

        setupSortButton()
        setupActions()

        loadData()
        renderCategoryChips()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        renderCategoryChips()
        refresh()
    }

    private fun setupSortButton() {
        binding.btnSortSelector.text = SORT_LABELS[sortMode]
        binding.btnSortSelector.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择排序方式")
                .setSingleChoiceItems(SORT_LABELS, sortMode) { dialog, which ->
                    sortMode = which
                    binding.btnSortSelector.text = SORT_LABELS[sortMode]
                    refresh()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupActions() {
        binding.btnManageCategories.setOnClickListener {
            CategoryManagerDialog.showManageDialog(requireContext(), store) {
                renderCategoryChips()
                refresh()
            }
        }
    }

    private fun loadData() {
        entries.clear()
        entries.addAll(store.loadAll())
    }

    /** 动态渲染横向滑动的分类筛选药丸 (Chips) */
    private fun renderCategoryChips() {
        val container = binding.categoryChipsContainer
        container.removeAllViews()

        val allCategories = listOf("全部") + store.getCategories()

        for (cat in allCategories) {
            val chip = TextView(requireContext()).apply {
                text = cat
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6))
                val isSelected = (selectedCategory == cat)

                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_chip_active)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                } else {
                    setBackgroundResource(R.drawable.bg_chip_inactive)
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }

                isClickable = true
                isFocusable = true
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
                layoutParams = params

                setOnClickListener {
                    selectedCategory = cat
                    renderCategoryChips()
                    refresh()
                }
            }
            container.addView(chip)
        }

        // + 自定义快捷药丸
        val addCustomChip = TextView(requireContext()).apply {
            text = "+ 自定义"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            setBackgroundResource(R.drawable.bg_btn_custom_add)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                CategoryManagerDialog.showAddCategoryDialog(requireContext(), store) {
                    renderCategoryChips()
                    refresh()
                }
            }
        }
        container.addView(addCustomChip)
    }

    /** 构建分组数据并依据设定排序 */
    private fun buildGroups(): List<CategoryGroup> {
        val configuredCats = store.getCategories()
        val data = LinkedHashMap<String, LinkedHashMap<String, BrandData>>()

        for (cat in configuredCats) {
            data[cat] = linkedMapOf()
        }

        for (e in entries) {
            val cat = e.category
            if (!data.containsKey(cat)) data[cat] = linkedMapOf()
            val brands = data[cat]!!
            if (!brands.containsKey(e.brand)) brands[e.brand] = BrandData()
            val bd = brands[e.brand]!!
            bd.unit = e.unit.ifEmpty { "片" }
            if (e.isIn) {
                bd.addCount += e.qty
                bd.addAmount += e.qty * e.price
            } else {
                bd.reduceCount += e.qty
            }
        }

        val result = mutableListOf<CategoryGroup>()
        for ((cat, brands) in data) {
            if (brands.isEmpty()) continue
            val brandList = brands.entries.map { (name, bd) ->
                val stock = bd.addCount - bd.reduceCount
                val avg = if (bd.addCount > 0) bd.addAmount / bd.addCount else 0.0
                BrandSummary(name, stock, bd.addAmount, avg, bd.unit)
            }.sortedWith(when (sortMode) {
                1 -> compareByDescending<BrandSummary> { it.count }
                2 -> compareBy<BrandSummary> { it.count }
                3 -> compareByDescending<BrandSummary> { it.amount }
                4 -> compareBy<BrandSummary> { it.name }
                else -> compareByDescending<BrandSummary> { it.count }
            })

            if (brandList.isNotEmpty()) {
                val total = brandList.sumOf { it.count }
                val amount = brandList.sumOf { it.amount }
                val u = brandList.firstOrNull()?.unit ?: "片"
                result.add(CategoryGroup(cat, brandList, total, amount, u))
            }
        }

        val sortedResult = when (sortMode) {
            0 -> result
            1 -> result.sortedByDescending { it.totalCount }
            2 -> result.sortedBy { it.totalCount }
            3 -> result.sortedByDescending { it.totalAmount }
            4 -> result.sortedBy { it.name }
            else -> result
        }

        return if (selectedCategory != "全部") {
            sortedResult.filter { it.name == selectedCategory }
        } else {
            sortedResult
        }
    }

    private fun refresh() {
        val groups = buildGroups()

        val grandCount = entries.filter { it.isIn }.sumOf { it.qty } - entries.filter { !it.isIn }.sumOf { it.qty }
        val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }
        val distinctBrands = entries.map { it.brand }.distinct().size

        binding.totalCount.text = grandCount.coerceAtLeast(0).toString()
        binding.totalAmount.text = "¥${String.format(Locale.getDefault(), "%.2f", totalSpent)}"
        binding.brandCoverageText.text = "$distinctBrands 个品牌"

        binding.filterSummaryText.text = if (selectedCategory == "全部") {
            "全部库存明细 (共 ${groups.sumOf { it.brands.size }} 个品牌项)"
        } else {
            "【$selectedCategory】分类下共 ${groups.find { it.name == selectedCategory }?.brands?.size ?: 0} 个品牌"
        }

        binding.emptyState.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        binding.categoryList.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE

        adapter.submit(groups)
    }

    /** 品牌快捷操作弹窗 */
    private fun showBrandActionSheet(brand: BrandSummary, category: String) {
        val u = brand.unit.ifEmpty { "片" }
        val options = arrayOf(
            "➕ 极速补货 (+1 $u)",
            "➖ 极速消耗 (-1 $u)",
            "📝 自定义数量记一笔"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("【$category】${brand.name} (当前: ${brand.count} $u)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val newEntry = Entry(
                            category = category,
                            brand = brand.name,
                            qty = 1,
                            price = brand.avgPrice,
                            ts = System.currentTimeMillis(),
                            isIn = true,
                            notes = "快捷补货",
                            unit = u
                        )
                        entries.add(newEntry)
                        store.saveAll(entries)
                        refresh()
                        Toast.makeText(requireContext(), "已为【${brand.name}】补货 +1 $u", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val newEntry = Entry(
                            category = category,
                            brand = brand.name,
                            qty = 1,
                            price = 0.0,
                            ts = System.currentTimeMillis(),
                            isIn = false,
                            notes = "快捷消耗",
                            unit = u
                        )
                        entries.add(newEntry)
                        store.saveAll(entries)
                        refresh()
                        Toast.makeText(requireContext(), "已记录【${brand.name}】消耗 -1 $u", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        (activity as? MainActivity)?.showAddDialog(
                            prefillBrand = brand.name,
                            prefillCategory = category
                        )
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class BrandData(
        var addCount: Int = 0,
        var addAmount: Double = 0.0,
        var reduceCount: Int = 0,
        var unit: String = "片"
    )
}
