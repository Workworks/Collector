package com.kfaino.diapertracker

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var categoryBreakdownAdapter: CategoryBreakdownAdapter
    private lateinit var monthAdapter: MonthStatAdapter

    private var selectedSubTab = 0 // 0=概览, 1=资产报表, 2=账本报表

    data class CategoryStat(
        val name: String,
        var count: Int,
        var amount: Double,
        var color: Int = 0,
        var percent: Double = 0.0
    )

    data class CategoryBreakdownItem(
        val name: String,
        val count: Int,
        val amount: Double,
        val color: Int,
        val percent: Double
    )

    class CategoryBreakdownAdapter : RecyclerView.Adapter<CategoryBreakdownAdapter.VH>() {
        private var items: List<CategoryBreakdownItem> = emptyList()

        fun submit(list: List<CategoryBreakdownItem>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemAssetCategoryBreakdownBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val b = holder.binding

            b.catDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.color)
            }
            b.catName.text = item.name
            b.catPercent.text = String.format(Locale.getDefault(), "%.1f%%", item.percent)
            b.catAmountQty.text = "¥${String.format(Locale.getDefault(), "%.2f", item.amount)} · ${item.count}件"
        }

        class VH(val binding: ItemAssetCategoryBreakdownBinding) : RecyclerView.ViewHolder(binding.root)
    }

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

        categoryBreakdownAdapter = CategoryBreakdownAdapter()
        binding.categoryBreakdownList.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryBreakdownList.adapter = categoryBreakdownAdapter

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
        binding.tabOverview.applyPressScaleAnimation(0.92f)
        binding.tabAssets.applyPressScaleAnimation(0.92f)
        binding.tabAccount.applyPressScaleAnimation(0.92f)

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

        binding.containerOverview.visibility = if (tab == 0) View.VISIBLE else View.GONE
        binding.containerAssets.visibility = if (tab == 1) View.VISIBLE else View.GONE
        binding.containerAccount.visibility = if (tab == 2) View.VISIBLE else View.GONE
    }

    private fun setupShareButton() {
        binding.btnShareReport.applyPressScaleAnimation(0.90f)
        binding.btnShareReport.setOnClickListener {
            val options = arrayOf(
                "📝 分享报表摘要文本",
                "📊 导出【资产全景总表】(CSV / Excel 兼容)",
                "📋 导出【收支流水明细】(CSV / Excel 兼容)"
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("分享与导出报表")
                .setItems(options) { _, which ->
                    val entries = store.loadAll()
                    when (which) {
                        0 -> {
                            val totalInCount = entries.filter { it.isIn }.sumOf { it.qty }
                            val totalOutCount = entries.filter { !it.isIn }.sumOf { it.qty }
                            val inStock = (totalInCount - totalOutCount).coerceAtLeast(0)
                            val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

                            val shareText = """
                                📊 【Collecter 资产与收纳数据报表】
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
                        1 -> {
                            ExportManager.exportAndShareAssetsCsv(requireActivity(), entries)
                        }
                        2 -> {
                            ExportManager.exportAndShareTimelineCsv(requireActivity(), entries)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    fun refresh() {
        if (_binding == null) return
        val entries = store.loadAll()

        // 1. 全局指标核算
        val totalInCount = entries.filter { it.isIn }.sumOf { it.qty }
        val totalOutCount = entries.filter { !it.isIn }.sumOf { it.qty }
        val currentStock = (totalInCount - totalOutCount).coerceAtLeast(0)
        val totalSpent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

        val overallAvg = if (totalInCount > 0) totalSpent / totalInCount else 0.0
        val currentInStockWorth = currentStock * overallAvg

        // 2. 填充概览 Tab
        binding.statNetWorth.text = "¥ ${String.format(Locale.getDefault(), "%.2f", totalSpent)}"
        binding.statDailyHold.text = "$currentStock"

        val catMap = LinkedHashMap<String, CategoryStat>()
        val palette = intArrayOf(
            Color.parseColor("#3B82F6"), Color.parseColor("#10B981"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#F59E0B"),
            Color.parseColor("#EC4899"), Color.parseColor("#06B6D4"),
            Color.parseColor("#F97316"), Color.parseColor("#6366F1")
        )

        for (e in entries) {
            val stat = catMap.getOrPut(e.category) { CategoryStat(e.category, 0, 0.0) }
            if (e.isIn) {
                stat.count += e.qty
                stat.amount += e.qty * e.price
            } else {
                stat.count -= e.qty
            }
        }

        val catList = catMap.values.filter { it.count > 0 || it.amount > 0 }.toList()
        val totalStockAll = catList.sumOf { it.count.coerceAtLeast(0) }.coerceAtLeast(1)

        val breakdownList = catList.mapIndexed { idx, stat ->
            val color = palette[idx % palette.size]
            val pct = (stat.count.coerceAtLeast(0) * 100.0 / totalStockAll).coerceIn(0.0, 100.0)
            stat.color = color
            stat.percent = pct
            CategoryBreakdownItem(
                name = stat.name,
                count = stat.count,
                amount = stat.amount,
                color = color,
                percent = pct
            )
        }.sortedByDescending { it.amount }

        categoryBreakdownAdapter.submit(breakdownList)

        // 概览分类动态环形图
        val slices = breakdownList.map {
            DonutChartView.Slice(
                name = it.name,
                value = it.amount,
                color = it.color
            )
        }
        binding.overviewDonutChart.setData(slices, animate = true)

        // 概览多色横条
        val barContainer = binding.overviewCategoryProgressBar
        barContainer.removeAllViews()
        for (item in breakdownList) {
            if (item.percent > 0) {
                val segment = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, item.percent.toFloat())
                    setBackgroundColor(item.color)
                }
                barContainer.addView(segment)
            }
        }

        // 3. 闲置资产与断舍离健康雷达
        val activeNonSubs = entries.filter { !it.isRetired && !it.isSubscription }
        val nowMs = System.currentTimeMillis()
        val idleItems = activeNonSubs.filter { item ->
            val isUncheckedLong = (item.getDaysOwned() > 180 && (nowMs - item.lastCheckedAt > 180L * 24 * 60 * 60 * 1000))
            val isExpiringSoon = (item.assetType == "expiring" && item.expiryDate > 0 && item.expiryDate - nowMs < 15L * 24 * 60 * 60 * 1000)
            isUncheckedLong || isExpiringSoon
        }

        val healthScore = (100 - idleItems.size * 5).coerceIn(40, 100)
        binding.tvHealthScoreBadge.text = "资产健康度 ${healthScore}分"
        if (healthScore >= 90) {
            binding.tvHealthScoreBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.stock_healthy_text))
            binding.tvHealthScoreBadge.setBackgroundResource(R.drawable.bg_stock_healthy)
        } else {
            binding.tvHealthScoreBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
            binding.tvHealthScoreBadge.setBackgroundResource(R.drawable.bg_chip_inactive)
        }

        binding.tvHealthScoreBadge.applyPressScaleAnimation(0.92f)
        binding.tvHealthScoreBadge.setOnClickListener {
            ResaleCopilotHelper.showDeclutterCabinDialog(requireActivity(), store) { refresh() }
        }

        if (idleItems.isEmpty()) {
            binding.tvHealthDesc.text = "🎉 资产流转极其健康！暂无超过 180 天未打卡或临期闲置物品。"
            binding.idleItemsContainer.removeAllViews()
            binding.idleItemsContainer.visibility = View.GONE
        } else {
            binding.tvHealthDesc.text = "发现 ${idleItems.size} 件超 180 天未打卡确认或临期闲置物品，点击进入决策舱集中回血出清："
            binding.idleItemsContainer.visibility = View.VISIBLE
            binding.idleItemsContainer.removeAllViews()

            for (item in idleItems.take(5)) {
                val rowCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                    radius = 24f
                    cardElevation = 0f
                    strokeWidth = 2
                    setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.input_bg))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = 16
                    layoutParams = lp
                }

                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(28, 20, 28, 20)
                }

                val tvName = TextView(requireContext()).apply {
                    text = "📦 ${item.brand}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    paint.isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val btnConfirmChecked = TextView(requireContext()).apply {
                    text = "✅ 确认在位"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                    setBackgroundResource(R.drawable.bg_btn_custom_add)
                    setPadding(20, 10, 20, 10)
                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        store.confirmItemChecked(item.id)
                        refresh()
                    }
                }

                val btnRetire = TextView(requireContext()).apply {
                    text = "♻️ 出清回血"
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_btn_primary)
                    setPadding(20, 10, 20, 10)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.marginStart = 16
                    layoutParams = lp
                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        ResaleCopilotHelper.showDeclutterCabinDialog(requireActivity(), store) { refresh() }
                    }
                }

                row.addView(tvName)
                row.addView(btnConfirmChecked)
                row.addView(btnRetire)
                rowCard.addView(row)
                binding.idleItemsContainer.addView(rowCard)
            }
        }

        // 当月收支透视
        val now = Calendar.getInstance()
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now.time)
        val monthNum = now.get(Calendar.MONTH) + 1

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

        // 3. 填充【资产报表】Tab
        binding.assetTotalSpent.text = "¥${String.format(Locale.getDefault(), "%.2f", totalSpent)}"
        binding.assetInStockWorth.text = "¥${String.format(Locale.getDefault(), "%.2f", currentInStockWorth)}"

        // 3.1 闲置变现与回血 ROI 分析看板
        val resale = store.getResaleAnalytics()
        binding.tvResaleTotalRecovered.text = "¥${String.format(Locale.getDefault(), "%.2f", resale.totalRecovered)}"
        binding.tvResaleRecoveryRate.text = String.format(Locale.getDefault(), "%.1f%%", resale.recoveryRate)
        binding.tvResaleNetCostDesc.text = "退役物品总原值 ¥${String.format(Locale.getDefault(), "%.2f", resale.totalInvested)}，已回收变现 ¥${String.format(Locale.getDefault(), "%.2f", resale.totalRecovered)}，实际净支出 ¥${String.format(Locale.getDefault(), "%.2f", resale.netCost)}"

        val heroContainer = binding.layoutResaleHeroContainer
        heroContainer.removeAllViews()
        if (resale.soldItems.isEmpty()) {
            val emptyHeroTv = TextView(requireContext()).apply {
                text = "💡 暂无转卖回血记录。物品退役时标记「挂闲鱼代售」或「转转二手」并记录卖出金额，即可在此查看资金回血榜！"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 8, 0, 8)
                setLineSpacing(0f, 1.2f)
            }
            heroContainer.addView(emptyHeroTv)
        } else {
            val heroTitle = TextView(requireContext()).apply {
                text = "🏆 断舍离变现英雄榜 (Top 5)"
                textSize = 13f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setPadding(0, 4, 0, 8)
            }
            heroContainer.addView(heroTitle)

            for ((hIdx, sItem) in resale.soldItems.take(5).withIndex()) {
                val heroRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 6, 0, 6)
                }

                val hRankTv = TextView(requireContext()).apply {
                    text = "${hIdx + 1}."
                    textSize = 13f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(context, R.color.accent_dark))
                    setPadding(0, 0, 8, 0)
                }

                val hNameTv = TextView(requireContext()).apply {
                    text = "${sItem.brand} (${sItem.retiredAction.ifBlank { "已出二手" }})"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val originalCost = sItem.price * sItem.qty
                val roiPct = if (originalCost > 0) (sItem.retiredSoldPrice / originalCost) * 100.0 else 0.0
                val hPriceTv = TextView(requireContext()).apply {
                    text = "回血 ¥${String.format(Locale.getDefault(), "%.2f", sItem.retiredSoldPrice)} (${String.format(Locale.getDefault(), "%.0f%%", roiPct)})"
                    textSize = 12f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                }

                heroRow.addView(hRankTv)
                heroRow.addView(hNameTv)
                heroRow.addView(hPriceTv)
                heroContainer.addView(heroRow)
            }
        }

        // Top 5 价值品牌榜
        val brandMap = LinkedHashMap<String, Pair<Int, Double>>()
        for (e in entries) {
            val cur = brandMap.getOrDefault(e.brand, Pair(0, 0.0))
            if (e.isIn) {
                brandMap[e.brand] = Pair(cur.first + e.qty, cur.second + (e.qty * e.price))
            } else {
                brandMap[e.brand] = Pair(cur.first - e.qty, cur.second)
            }
        }

        val topBrands = brandMap.entries
            .filter { it.value.first > 0 || it.value.second > 0 }
            .sortedByDescending { it.value.second }
            .take(5)

        val topBrandsContainer = binding.assetTopBrandsContainer
        topBrandsContainer.removeAllViews()

        val medals = listOf("🥇", "🥈", "🥉", "4️⃣", "5️⃣")
        for ((idx, tb) in topBrands.withIndex()) {
            val brandRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val medalTv = TextView(requireContext()).apply {
                text = if (idx < medals.size) medals[idx] else "${idx + 1}"
                textSize = 16f
                setPadding(0, 0, 12, 0)
            }

            val nameTv = TextView(requireContext()).apply {
                text = tb.key
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val worthTv = TextView(requireContext()).apply {
                text = "在库 ${tb.value.first} 件 · 估值 ¥${String.format(Locale.getDefault(), "%.2f", tb.value.second)}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
            }

            brandRow.addView(medalTv)
            brandRow.addView(nameTv)
            brandRow.addView(worthTv)
            topBrandsContainer.addView(brandRow)
        }

        // 缺货与库存告急面板
        val lowStockBrands = brandMap.entries.filter { it.value.first <= 0 }
        val lowStockContainer = binding.assetLowStockContainer
        lowStockContainer.removeAllViews()

        if (lowStockBrands.isEmpty()) {
            val emptyAlertTv = TextView(requireContext()).apply {
                text = "✅ 暂无库存告急或缺货项目，所有收纳物品均处于充足在库状态。"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 10, 0, 10)
            }
            lowStockContainer.addView(emptyAlertTv)
        } else {
            for (ls in lowStockBrands) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val nameTv = TextView(requireContext()).apply {
                    text = "🚨 ${ls.key}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.danger))
                    paint.isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countTv = TextView(requireContext()).apply {
                    text = "已缺货 (${ls.value.first})"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.danger))
                }

                row.addView(nameTv)
                row.addView(countTv)
                lowStockContainer.addView(row)
            }
        }

        // 4. 填充【账本报表】Tab
        binding.accountTotalSpent.text = "¥${String.format(Locale.getDefault(), "%.2f", totalSpent)}"
        binding.accountTotalConsumed.text = "$totalOutCount"
        binding.accountAvgPrice.text = "¥${String.format(Locale.getDefault(), "%.2f", overallAvg)}"

        val calInstance = Calendar.getInstance()
        val monthGroups = LinkedHashMap<String, MutableList<Entry>>()
        for (e in entries) {
            val key = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(e.ts))
            monthGroups.getOrPut(key) { mutableListOf() }.add(e)
        }

        val monthList = monthGroups.map { (_, list) ->
            val firstEntryDate = Date(list.first().ts)
            calInstance.time = firstEntryDate
            val y = calInstance.get(Calendar.YEAR)
            val m = calInstance.get(Calendar.MONTH) + 1
            val adds = list.filter { it.isIn }
            val reduces = list.filter { !it.isIn }
            val topItems = list.map { it.brand }.distinct().take(3)
            MonthStat(
                year = y,
                month = m,
                addCount = adds.sumOf { it.qty },
                addAmount = adds.sumOf { it.qty * it.price },
                reduceCount = reduces.sumOf { it.qty },
                entryCount = list.size,
                topItems = topItems
            )
        }.sortedWith(compareByDescending<MonthStat> { it.year }.thenByDescending { it.month })

        monthAdapter.submit(monthList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
