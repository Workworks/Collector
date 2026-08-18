package com.kfaino.diapertracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.FragmentTimelineBinding

/** 生活流：按时间倒序展示所有记录，支持修改与删除 */
class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter(
            onEdit = { entry, pos ->
                val allEntries = store.loadAll()
                val realIndex = allEntries.indexOf(entry)
                if (realIndex != -1) {
                    (activity as? MainActivity)?.showAddDialog(
                        prefillBrand = entry.brand,
                        prefillCategory = entry.category,
                        editEntry = entry,
                        editPosition = realIndex
                    )
                }
            },
            onDelete = { entry, _ -> confirmDelete(entry) }
        )
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        if (_binding == null) return
        val entries = store.loadAll()
        binding.timelineCountBadge.text = "共 ${entries.size} 条记录"
        adapter.submit(entries.reversed())
        binding.emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.historyList.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(entry: Entry) {
        val typeName = if (entry.isIn) "入库记录" else "消耗记录"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条【${entry.category} · ${entry.brand}】的 $typeName 吗？")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                val list = store.loadAll().toMutableList()
                list.remove(entry)
                store.saveAll(list)
                refresh()
                Toast.makeText(requireContext(), "已删除该条记录", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}