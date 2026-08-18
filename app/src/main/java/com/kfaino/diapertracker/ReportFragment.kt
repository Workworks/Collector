package com.kfaino.diapertracker

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kfaino.diapertracker.databinding.FragmentReportBinding
import com.kfaino.diapertracker.databinding.ItemAssetCategoryBreakdownBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private lateinit var monthAdapter: MonthStatAdapter

    private var selectedSubTab = 0 // 0=概览, 1=资产报表, 2=账本报表

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        monthAdapter = MonthStatAdapter()
        binding.monthRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.monthRecycler.adapter = monthAdapter

        setupSubTabs()
        setupShareButton()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun setupSubTabs() {
        binding.tabOverview.setOnClickListener { switchSubTab(0) }
        binding.tabAssets.setOnClickListener { switchSubTab(1) }
        binding.tabAccount.setOnClickListener { switchSubTab(2) }
    }

    private fun switchSubTab(tab: Int) {
        selectedSubTab = tab
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)

        binding.tabOverviewText.setTextColor(if (tab == 0) activeColor else inactiveColor)
        binding.tabOverviewText.paint.isFakeBoldText = (tab == 0)
        binding.tabOverviewIndicator.setBackgroundColor(if (tab == 0) primaryColor else Color.TRANSPARENT)

        binding.tabAssetsText.setTextColor(if (tab == 1) activeColor else inactiveColor)
        binding.tabAssetsText.paint.isFakeBoldText = (tab == 1)
        binding.tabAssetsIndicator.setBackgroundColor(if (tab == 1) primaryColor else Color.TRANSPARENT)

        binding.tabAccountText.setTextColor(if (tab == 2) activeColor else inactiveColor)
        binding.tabAccountText.paint.isFakeBoldText = (tab == 2)
        binding.tabAccountIndicator.setBackgroundColor(if (tab == 2) primaryColor else Color.TRANSPARENT)
    }

    private fun setupShareButton() {
        binding.btnShareReport.setOnClickListener {
            val entries = store.loadAll()
            val totalInCount = entries.filter { it.isIn }.sumOf { it.qty }
            val totalOutCount = entries.filter { !it.isIn }.sumOf { it.qty }
            val inStock = (totalInCount - totalOutCount).coerceAtLeast(0)
            val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

            val shareText = """
                📊 【Collecter 资产数据报表】
                · 净资产估值：¥${String.format(Locale.getDefault(), "%.2f", totalSpent)}
                · 在库总数量：$inStock (累计入库: $totalInCount, 累计消耗: $totalOutCount)
                · 分类总数：${store.getCategories().size} 类
                
                记录时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}
            """.trimIndent()

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "分享资产报表"))
        }
    }

    private fun refresh() {
        if (_binding == null) return
        val entries = store.loadAll()

        // 1. 全局资产统计
        val totalInCount = entries.filter { it.isIn }.sumOf { it.qty }
        val totalOutCount = entries.filter { !it.isIn }.sumOf { it.qty }
        val currentStock = (totalInCount - totalOutCount).coerceAtLeast(0)
        val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

        binding.statNetWorth.text = "¥ ${String.format(Locale.getDefault(), "%.2f", totalSpent)}"
        binding.statDailyHold.text = "$currentStock"

        // 2. 资产版图（按分类统计）
        val catMap = LinkedHashMap<String, CategoryStat>()
        val palette = intArrayOf(
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#10B981"), // Emerald
            Color.parseColor("#8B5CF6"), // Purple
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#06B6D4"), // Cyan
            Color.parseColor("#F97316"), // Orange
            Color.parseColor("#6366F1")  // Indigo
        )

        for (e in entries) {
            val cat = e.category
            if (!catMap.containsKey(cat)) {
                catMap[cat] = CategoryStat(name = cat)
            }
            val cs = catMap[cat]!!
            if (e.isIn) {
                cs.addQty += e.qty
                cs.amount += e.qty * e.price
            } else {
                cs.reduceQty += e.qty
            }
        }

        val activeCategories = catMap.values.filter { it.addQty > 0 || it.reduceQty > 0 }
            .sortedByDescending { it.amount }

        binding.categoryTypeCount.text = "${activeCategories.size} 类"
        binding.assetBreakdownContainer.removeAllViews()

        if (activeCategories.isNotEmpty()) {
            for ((index, catStat) in activeCategories.withIndex()) {
                val color = palette[index % palette.size]
                val itemBinding = ItemAssetCategoryBreakdownBinding.inflate(
                    layoutInflater, binding.assetBreakdownContainer, false
                )
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                itemBinding.catDot.background = dotBg
                itemBinding.catName.text = catStat.name

                val percent = if (totalSpent > 0) {
                    ((catStat.amount / totalSpent) * 100).toInt()
                } else 0
                itemBinding.catPercent.text = "$percent%"
                itemBinding.catAmountQty.text = "¥${String.format(Locale.getDefault(), "%.2f", catStat.amount)}"

                binding.assetBreakdownContainer.addView(itemBinding.root)
            }
        } else {
            // 空分类引导
            val emptyTip = android.widget.TextView(requireContext()).apply {
                text = "暂无分类资产数据，记一笔后自动生成"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 16, 0, 16)
            }
            binding.assetBreakdownContainer.addView(emptyTip)
        }

        // 3. 本月账本统计
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        val monthNum = cal.get(Calendar.MONTH) + 1

        val monthEntries = entries.filter {
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.ts)) == currentMonthKey
        }

        val monthSpent = monthEntries.filter { it.isIn }.sumOf { it.qty * it.price }
        val monthInQty = monthEntries.filter { it.isIn }.sumOf { it.qty }
        val monthOutQty = monthEntries.filter { !it.isIn }.sumOf { it.qty }

        binding.monthEntryBadge.text = "${monthNum}月 · ${monthEntries.size} 笔"
        binding.monthExpense.text = "¥${String.format(Locale.getDefault(), "%.2f", monthSpent)}"
        binding.monthIncome.text = "+$monthInQty"
        binding.monthBalance.text = "-$monthOutQty"

        val overallAvg = if (totalInCount > 0) totalSpent / totalInCount else 0.0
        binding.structureInvest.text = "在库 ¥${String.format(Locale.getDefault(), "%.2f", currentStock * overallAvg)}"
        binding.structureConsume.text = "消耗 $totalOutCount"
        binding.structureAvg.text = "均价 ¥${String.format(Locale.getDefault(), "%.2f", overallAvg)}"

        // 4. 历史月度列表
        val calInstance = Calendar.getInstance()
        val monthGroups = LinkedHashMap<String, MutableList<Entry>>()
        for (e in entries) {
            val key = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(e.ts))
            monthGroups.getOrPut(key) { mutableListOf() }.add(e)
        }

        val monthList = monthGroups.map { (key, list) ->
            val firstEntryDate = Date(list.first().ts)
            calInstance.time = firstEntryDate
            val y = calInstance.get(Calendar.YEAR)
            val m = calInstance.get(Calendar.MONTH) + 1
            MonthStat(
                year = y,
                month = m,
                addCount = list.filter { it.isIn }.sumOf { it.qty },
                addAmount = list.filter { it.isIn }.sumOf { it.qty * it.price },
                reduceCount = list.filter { !it.isIn }.sumOf { it.qty },
                entryCount = list.size
            )
        }.sortedWith(compareByDescending<MonthStat> { it.year }.thenByDescending { it.month })

        monthAdapter.submit(monthList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class CategoryStat(
        val name: String,
        var addQty: Int = 0,
        var reduceQty: Int = 0,
        var amount: Double = 0.0
    )
}
