package com.kfaino.diapertracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kfaino.diapertracker.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private val entries = mutableListOf<Entry>()
    private lateinit var adapter: CategoryAdapter

    // 当前筛选和排序
    private var selectedCategory = "全部"
    private var sortMode = 0 // 0=按分组排序, 1=按库存降序, 2=按库存升序, 3=按花费降序, 4=按名称

    companion object {
        val SORT_LABELS = listOf("按分组排序", "按库存降序", "按库存升序", "按花费降序", "按名称排序")
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

        adapter = CategoryAdapter()
        binding.categoryList.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryList.adapter = adapter

        setupSortSpinner()
        setupCategorySpinner()
        setupActions()

        loadData()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        setupCategorySpinner()
        loadData()
        refresh()
    }

    private fun setupSortSpinner() {
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, SORT_LABELS)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = sortAdapter
        binding.spinnerSort.setSelection(sortMode)
        binding.spinnerSort.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                sortMode = pos
                refresh()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupCategorySpinner() {
        val allCats = store.getCategories()
        val catOptions = listOf("全部分类") + allCats
        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, catOptions)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = catAdapter

        val curIndex = if (selectedCategory == "全部" || selectedCategory == "全部分类") {
            0
        } else {
            catOptions.indexOf(selectedCategory).coerceAtLeast(0)
        }
        binding.spinnerCategory.setSelection(curIndex)

        binding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedCategory = if (pos == 0) "全部" else catOptions[pos]
                refresh()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupActions() {
        // + 自定义分类快捷按钮
        binding.btnAddCustom.setOnClickListener {
            CategoryManagerDialog.showAddCategoryDialog(requireContext(), store) {
                setupCategorySpinner()
                refresh()
            }
        }

        // 分类与尺码管理入口
        binding.btnManageCategories.setOnClickListener {
            CategoryManagerDialog.showManageDialog(requireContext(), store) {
                setupCategorySpinner()
                refresh()
            }
        }
    }

    private fun loadData() {
        entries.clear()
        entries.addAll(store.loadAll())
    }

    /** 构建分组数据并依据设定排序 */
    private fun buildGroups(): List<CategoryGroup> {
        val configuredCats = store.getCategories()
        val data = LinkedHashMap<String, LinkedHashMap<String, BrandData>>()

        // 按配置的分类顺序初始化
        for (cat in configuredCats) {
            data[cat] = linkedMapOf()
        }

        for (e in entries) {
            val cat = e.category
            if (!data.containsKey(cat)) data[cat] = linkedMapOf()
            val brands = data[cat]!!
            if (!brands.containsKey(e.brand)) brands[e.brand] = BrandData()
            val bd = brands[e.brand]!!
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
                BrandSummary(name, stock, bd.addAmount, avg)
            }.sortedWith(when (sortMode) {
                1 -> compareByDescending<BrandSummary> { it.count }
                2 -> compareBy<BrandSummary> { it.count }
                3 -> compareByDescending<BrandSummary> { it.amount }
                4 -> compareBy<BrandSummary> { it.name }
                else -> compareByDescending<BrandSummary> { it.count } // 默认分组内按库存降序
            })

            if (brandList.isNotEmpty()) {
                val total = brandList.sumOf { it.count }
                val amount = brandList.sumOf { it.amount }
                result.add(CategoryGroup(cat, brandList, total, amount))
            }
        }

        // 分组间的排序
        val sortedResult = when (sortMode) {
            0 -> result // 保持用户自定义/预设分组顺序
            1 -> result.sortedByDescending { it.totalCount } // 按总库存降序
            2 -> result.sortedBy { it.totalCount } // 按总库存升序
            3 -> result.sortedByDescending { it.totalAmount } // 按总花费降序
            4 -> result.sortedBy { it.name } // 按分类名称字母/拼音
            else -> result
        }

        return if (selectedCategory != "全部" && selectedCategory != "全部分类") {
            sortedResult.filter { it.name == selectedCategory }
        } else {
            sortedResult
        }
    }

    private fun refresh() {
        val groups = buildGroups()

        val grandCount = entries.filter { it.isIn }.sumOf { it.qty } - entries.filter { !it.isIn }.sumOf { it.qty }
        val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

        binding.totalCount.text = grandCount.coerceAtLeast(0).toString()
        binding.totalAmount.text = "¥${String.format("%.2f", totalSpent)}"

        binding.emptyState.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        binding.categoryList.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE

        adapter.submit(groups)
    }

    private data class BrandData(
        var addCount: Int = 0,
        var addAmount: Double = 0.0,
        var reduceCount: Int = 0
    )
}
