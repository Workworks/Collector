package com.kfaino.diapertracker

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
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
            "⚡ 智能分组排序",
            "📉 按在库数量从多到少",
            "📈 按在库数量从少到多",
            "💰 按花费金额从多到少",
            "🔤 按名称字母排序"
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
        binding.brandRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.brandRecycler.adapter = adapter

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
        binding.btnSortOrder.applyPressScaleAnimation(0.92f)
        binding.btnSortOrder.text = SORT_LABELS[sortMode] + " ▾"
        binding.btnSortOrder.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择排序方式")
                .setSingleChoiceItems(SORT_LABELS, sortMode) { dialog, which ->
                    sortMode = which
                    binding.btnSortOrder.text = SORT_LABELS[sortMode] + " ▾"
                    refresh()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupActions() {
        binding.btnOpenFloorplanMap.applyPressScaleAnimation(0.92f)
        binding.btnOpenFloorplanMap.setOnClickListener {
            FloorPlanDialog.show(requireActivity(), store, isSelectMode = false)
        }

        binding.btnManageCategories.applyPressScaleAnimation(0.92f)
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

    /** 渲染重要物品与防丢订阅核对卡片 */
    private fun renderImportantItems() {
        val importantEntries = store.getImportantEntries()
        if (importantEntries.isEmpty()) {
            binding.cardImportantItemsTracker.visibility = View.GONE
            return
        }

        binding.cardImportantItemsTracker.visibility = View.VISIBLE
        binding.importantCountBadge.text = "共 ${importantEntries.size} 项关注"

        val container = binding.importantItemsListContainer
        container.removeAllViews()

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000

        for (e in importantEntries) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(6), 0, dpToPx(6))
            }

            val iconTv = TextView(requireContext()).apply {
                text = "⭐"
                textSize = 14f
                setPadding(0, 0, dpToPx(8), 0)
            }

            val infoLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(requireContext()).apply {
                text = e.brand
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                paint.isFakeBoldText = true
            }

            val locText = if (e.location.isNotBlank()) "📍 ${e.houseName} · ${e.location}" else "📍 未标记放置位置"
            val lastCheckedText = if (e.lastCheckedAt > 0) {
                val daysAgo = ((now - e.lastCheckedAt) / dayMs).toInt()
                if (daysAgo == 0) "✅ 今日已核对" else "⚠️ 已 $daysAgo 天未核对"
            } else {
                "⚠️ 尚未核对位置"
            }

            val locTv = TextView(requireContext()).apply {
                text = "$locText  ($lastCheckedText)"
                textSize = 11f
                setTextColor(if (lastCheckedText.startsWith("✅")) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
            }

            infoLayout.addView(nameTv)
            infoLayout.addView(locTv)

            val checkBtn = TextView(requireContext()).apply {
                text = "✅ 确认在位"
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_chip_active)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                applyPressScaleAnimation(0.90f)
                setOnClickListener {
                    store.confirmItemChecked(e.id)
                    Toast.makeText(requireContext(), "已确认【${e.brand}】位置在位！", Toast.LENGTH_SHORT).show()
                    loadData()
                    renderImportantItems()
                }
            }

            row.addView(iconTv)
            row.addView(infoLayout)
            row.addView(checkBtn)
            container.addView(row)
        }
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

                applyPressScaleAnimation(0.92f)
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
            applyPressScaleAnimation(0.92f)
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
            bd.location = e.location
            bd.houseName = e.houseName
            bd.isImportant = e.isImportant
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
                BrandSummary(
                    name = name,
                    count = stock,
                    amount = bd.addAmount,
                    avgPrice = avg,
                    unit = bd.unit,
                    location = bd.location,
                    houseName = bd.houseName,
                    isImportant = bd.isImportant
                )
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
        binding.brandCoverageText.text = "$distinctBrands 种"

        val totalBrandsCount = groups.sumOf { it.brands.size }
        binding.groupCountText.text = if (selectedCategory == "全部") {
            "共 ${groups.size} 个分类，共 $totalBrandsCount 种物品"
        } else {
            "【$selectedCategory】分类下共 ${groups.find { it.name == selectedCategory }?.brands?.size ?: 0} 种物品"
        }

        binding.emptyLayout.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        binding.brandRecycler.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE

        adapter.submit(groups)
        renderImportantItems()
    }

    /** 品牌/物品快捷操作弹窗 */
    private fun showBrandActionSheet(brand: BrandSummary, category: String) {
        val u = brand.unit.ifEmpty { "片" }
        val locInfo = if (brand.location.isNotBlank()) "📍 放置于: ${brand.houseName} · ${brand.location}" else "📍 放置位置未填"
        val options = arrayOf(
            "➕ 极速入库 (+1 $u)",
            "➖ 极速消耗 (-1 $u)",
            "📍 查看该物品位置变迁时光轴",
            "🗺️ 在空间平面图上查看定位",
            "📝 自定义记一笔"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("【$category】${brand.name} (当前: ${brand.count} $u)\n$locInfo")
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
                            notes = "快捷入库",
                            unit = u,
                            location = brand.location,
                            houseName = brand.houseName
                        )
                        entries.add(newEntry)
                        store.saveAll(entries)
                        refresh()
                        Toast.makeText(requireContext(), "已成功补货 +1 $u", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (brand.count <= 0) {
                            Toast.makeText(requireContext(), "当前在库已为 0，消耗记录将使在库变为负数", Toast.LENGTH_SHORT).show()
                        }
                        val newEntry = Entry(
                            category = category,
                            brand = brand.name,
                            qty = 1,
                            price = brand.avgPrice,
                            ts = System.currentTimeMillis(),
                            isIn = false,
                            notes = "快捷出库消耗",
                            unit = u,
                            location = brand.location,
                            houseName = brand.houseName
                        )
                        entries.add(newEntry)
                        store.saveAll(entries)
                        refresh()
                        Toast.makeText(requireContext(), "已成功消耗 -1 $u", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val entry = entries.lastOrNull { it.brand == brand.name && it.category == category } ?: Entry(category = category, brand = brand.name, qty = brand.count)
                        LocationHistoryDialog.show(requireActivity(), entry)
                    }
                    3 -> {
                        FloorPlanDialog.show(requireActivity(), store, isSelectMode = false, currentHouseName = brand.houseName)
                    }
                    4 -> {
                        (activity as? MainActivity)?.showAddDialogWithInitial(category, brand.name, u)
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

    private class BrandData {
        var addCount: Int = 0
        var addAmount: Double = 0.0
        var reduceCount: Int = 0
        var unit: String = "片"
        var location: String = ""
        var houseName: String = "我的家"
        var isImportant: Boolean = false
    }
}
