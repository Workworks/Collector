package com.kfaino.diapertracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemMonthStatBinding
import java.util.Locale

class MonthStatAdapter : RecyclerView.Adapter<MonthStatAdapter.VH>() {

    private var items: List<MonthStat> = emptyList()

    fun submit(list: List<MonthStat>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMonthStatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.binding.monthLabel.text = String.format(Locale.getDefault(), "%d年%d月", m.year, m.month)
        holder.binding.monthAdd.text = "+${m.addCount}"
        holder.binding.monthReduce.text = "-${m.reduceCount}"
        holder.binding.monthAmount.text = "¥${String.format(Locale.getDefault(), "%.2f", m.addAmount)}"
    }

    class VH(val binding: ItemMonthStatBinding) : RecyclerView.ViewHolder(binding.root)
}
