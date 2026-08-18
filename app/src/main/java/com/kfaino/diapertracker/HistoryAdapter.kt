package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.view.LayoutInflater
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
        b.brand.text = "${entry.category} · ${entry.brand}"
        val sign = if (entry.isIn) "+" else "-"
        b.badge.text = sign
        b.badge.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(holder.itemView.context, if (entry.isIn) R.color.primary else R.color.accent)
        )
        b.time.text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(entry.ts))
        if (entry.isIn) {
            b.amount.text = "¥${String.format(Locale.getDefault(), "%.2f", entry.qty * entry.price)}"
            b.amount.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary))
            b.qtyInfo.text = if (entry.price > 0)
                "${entry.qty} × ¥${String.format(Locale.getDefault(), "%.2f", entry.price)}"
            else
                "${entry.qty} 件"
        } else {
            b.amount.text = "-${entry.qty}"
            b.amount.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent))
            b.qtyInfo.text = ""
        }
        b.deleteBtn.setOnClickListener { onDelete?.invoke(entry) }
    }

    class VH(val binding: RowHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}