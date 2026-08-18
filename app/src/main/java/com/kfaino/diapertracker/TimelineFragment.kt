package com.kfaino.diapertracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.FragmentTimelineBinding

/** 生活流：按时间倒序展示所有记录 */
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

        adapter = HistoryAdapter(onDelete = { entry -> confirmDelete(entry) })
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val entries = store.loadAll()
        adapter.submit(entries.reversed())
        binding.emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.historyList.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(entry: Entry) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_entry)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                val list = store.loadAll().toMutableList()
                list.remove(entry)
                store.saveAll(list)
                refresh()
            }
            .show()
    }
}