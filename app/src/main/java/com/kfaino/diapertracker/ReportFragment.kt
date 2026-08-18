package com.kfaino.diapertracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kfaino.diapertracker.databinding.FragmentReportBinding
import java.util.Calendar
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private lateinit var monthAdapter: MonthStatAdapter

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
        binding.reportMonthList.layoutManager = LinearLayoutManager(requireContext())
        binding.reportMonthList.adapter = monthAdapter

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val entries = store.loadAll()

        if (entries.isEmpty()) {
            binding.reportEmpty.visibility = View.VISIBLE
            binding.reportMonthList.visibility = View.GONE
            binding.reportStock.text = "0"
            binding.reportSpent.text = "¥0.00"
            binding.reportAvg.text = "0"
            binding.reportMonthAdd.text = "+0"
            binding.reportMonthReduce.text = "-0"
            binding.reportMonthEntries.text = "0"
            return
        }

        binding.reportEmpty.visibility = View.GONE
        binding.reportMonthList.visibility = View.VISIBLE

        // 当前库存
        val totalAdd = entries.filter { it.isIn }.sumOf { it.qty }
        val totalReduce = entries.filter { !it.isIn }.sumOf { it.qty }
        val stock = (totalAdd - totalReduce).coerceAtLeast(0)
        val spent = entries.filter { it.isIn }.sumOf { it.qty * it.price }

        // 日均消耗：按最早记录到今天的天数算
        val firstTs = entries.minOfOrNull { it.ts } ?: System.currentTimeMillis()
        val daysSinceFirst = ((System.currentTimeMillis() - firstTs) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val avgDaily = if (daysSinceFirst > 0) String.format("%.1f", totalReduce.toDouble() / daysSinceFirst) else "0"

        binding.reportStock.text = stock.toString()
        binding.reportSpent.text = "¥${String.format("%.2f", spent)}"
        binding.reportAvg.text = avgDaily

        // 月度统计
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1

        val monthStats = buildMonthStats(entries)

        // 本月统计
        val thisMonth = monthStats.find { it.year == currentYear && it.month == currentMonth }
        val monthTitle = String.format(Locale.getDefault(), "%d年%d月", currentYear, currentMonth)
        binding.reportMonthTitle.text = monthTitle
        binding.reportMonthAdd.text = "+${thisMonth?.addCount ?: 0}"
        binding.reportMonthReduce.text = "-${thisMonth?.reduceCount ?: 0}"
        binding.reportMonthEntries.text = "${thisMonth?.entryCount ?: 0}"

        // 历史月份列表（排除本月，按时间倒序）
        val history = monthStats.filter { !(it.year == currentYear && it.month == currentMonth) }
            .sortedByDescending { it.year * 100 + it.month }
        monthAdapter.submit(history)
    }

    /** 按月汇总 */
    private fun buildMonthStats(entries: List<Entry>): List<MonthStat> {
        val map = LinkedHashMap<String, MonthStat>()

        for (e in entries) {
            val cal = Calendar.getInstance().apply { timeInMillis = e.ts }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val key = "$y-${String.format(Locale.getDefault(), "%02d", m)}"

            val stat = map.getOrPut(key) { MonthStat(y, m, 0, 0.0, 0, 0) }
            // rebuild mutable copy
            val mutable = stat.copy()
            val newStat = MonthStat(
                year = mutable.year,
                month = mutable.month,
                addCount = mutable.addCount + if (e.isIn) e.qty else 0,
                addAmount = mutable.addAmount + if (e.isIn) e.qty * e.price else 0.0,
                reduceCount = mutable.reduceCount + if (!e.isIn) e.qty else 0,
                entryCount = mutable.entryCount + 1
            )
            map[key] = newStat
        }
        return map.values.toList()
    }
}
