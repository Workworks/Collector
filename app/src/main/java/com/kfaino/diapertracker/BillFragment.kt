package com.kfaino.diapertracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.FragmentBillsBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class BillFragment : Fragment() {
    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!
    private val store by lazy { BillStore(requireContext()) }
    private var month: YearMonth = YearMonth.now()
    private lateinit var adapter: BillAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = BillAdapter(::edit, ::confirmDelete, ::markPosted)
        binding.billList.layoutManager = LinearLayoutManager(requireContext())
        binding.billList.adapter = adapter
        binding.btnBillPrevMonth.applyPressScaleAnimation(0.92f)
        binding.btnBillNextMonth.applyPressScaleAnimation(0.92f)
        binding.btnBillQuickInput.applyPressScaleAnimation(0.94f)
        binding.btnBillManual.applyPressScaleAnimation(0.94f)
        binding.btnBillReports.applyPressScaleAnimation(0.92f)
        binding.btnBillPrevMonth.setOnClickListener { month = month.minusMonths(1); refresh() }
        binding.btnBillNextMonth.setOnClickListener { month = month.plusMonths(1); refresh() }
        binding.btnBillQuickInput.setOnClickListener { openQuickEntry() }
        binding.btnBillManual.setOnClickListener { openManualEntry() }
        binding.btnBillFormatHelp.setOnClickListener { BillQuickEntryDialog.showFormat(requireActivity() as AppCompatActivity) }
        binding.btnBillReports.setOnClickListener { (activity as? MainActivity)?.navigateToLegacyTab(2) }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refresh()
    }

    fun openManualEntry() {
        val host = activity as? AppCompatActivity ?: return
        BillEntryDialog.show(host, store) { refresh() }
    }

    private fun openQuickEntry() {
        val host = activity as? AppCompatActivity ?: return
        BillQuickEntryDialog.show(host, store) { refresh() }
    }

    private fun edit(record: BillRecord) {
        val host = activity as? AppCompatActivity ?: return
        BillEntryDialog.show(host, store, record) { refresh() }
    }

    private fun confirmDelete(record: BillRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除这笔账单？")
            .setMessage("${record.title} · ${BillAmounts.format(record.amountMinor, record.currency)}\n删除后可通过已有备份恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                runStoreAction("删除账单失败，原数据已保留") { store.delete(record.id) }
            }
            .show()
    }

    private fun markPosted(record: BillRecord) {
        runStoreAction("更新入账状态失败") { store.markPosted(record.id) }
    }

    private fun runStoreAction(message: String, action: () -> Unit) {
        try {
            action()
            refresh()
        } catch (e: Exception) {
            Log.e("BillFragment", "$message: month=$month", e)
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    fun refresh() {
        if (_binding == null) return
        binding.billMonthLabel.text = month.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.SIMPLIFIED_CHINESE))
        try {
            val records = store.forMonth(month)
            val summaries = store.summarize(records)
            binding.billMonthExpense.text = if (summaries.isEmpty()) "¥0.00" else summaries.joinToString("   ") {
                BillAmounts.format(it.postedExpenseMinor, it.currency)
            }
            val pending = summaries.filter { it.pendingMinor > 0 }.joinToString(" · ") {
                "待入账 ${BillAmounts.format(it.pendingMinor, it.currency)}"
            }.ifBlank { "暂无待入账" }
            val income = summaries.filter { it.postedIncomeMinor > 0 }.joinToString(" · ") {
                "收入 ${BillAmounts.format(it.postedIncomeMinor, it.currency)}"
            }
            binding.billMonthMeta.text = listOf("${records.size} 笔", pending, income).filter(String::isNotBlank).joinToString(" · ")
            adapter.submit(records)
            binding.billEmptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            binding.billList.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            Log.e("BillFragment", "读取账单失败: month=$month", e)
            binding.billEmptyState.visibility = View.VISIBLE
            binding.billList.visibility = View.GONE
            binding.billMonthExpense.text = "读取失败"
            binding.billMonthMeta.text = "账单数据未被覆盖，请从备份或日志核对"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
