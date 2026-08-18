package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.RowHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onDelete: ((Entry) -> Unit)? = null
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(RowHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        val b = holder.binding
        val ctx = holder.itemView.context

        b.categoryTag.text = entry.category
        b.brand.text = entry.brand
        b.time.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.ts))

        if (entry.isIn) {
            b.badge.text = "+"
            b.badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
            b.amount.text = "+ ¥${String.format(Locale.getDefault(), "%.2f", entry.qty * entry.price)}"
            b.amount.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            b.qtyInfo.text = if (entry.price > 0)
                "${entry.qty} 件 · 单价 ¥${String.format(Locale.getDefault(), "%.2f", entry.price)}"
            else
                "${entry.qty} 件入库"
        } else {
            b.badge.text = "-"
            b.badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.danger))
            b.amount.text = "- ${entry.qty} 件"
            b.amount.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
            b.qtyInfo.text = "出库消耗"
        }

        if (entry.notes.isNotBlank()) {
            b.notesText.visibility = View.VISIBLE
            b.notesText.text = "备注: ${entry.notes}"
        } else {
            b.notesText.visibility = View.GONE
        }

        b.deleteBtn.setOnClickListener { onDelete?.invoke(entry) }
    }

    class VH(val binding: RowHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}