package com.kfaino.diapertracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemBillRecordBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class BillAdapter(
    private val onEdit: (BillRecord) -> Unit,
    private val onDelete: (BillRecord) -> Unit,
    private val onMarkPosted: (BillRecord) -> Unit
) : RecyclerView.Adapter<BillAdapter.ViewHolder>() {
    internal data class Row(val record: BillRecord, val showDateHeader: Boolean)
    private var rows: List<Row> = emptyList()

    fun submit(records: List<BillRecord>) {
        var lastDate = ""
        rows = records.map { record ->
            val date = dateKey(record)
            Row(record, date != lastDate).also { lastDate = date }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBillRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(rows[position])

    inner class ViewHolder(private val binding: ItemBillRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        internal fun bind(row: Row) {
            val item = row.record
            val context = binding.root.context
            val localDate = Instant.ofEpochMilli(item.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
            binding.billDateHeader.visibility = if (row.showDateHeader) View.VISIBLE else View.GONE
            binding.billDateHeader.text = localDate.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE))
            binding.billTitle.text = item.title
            val recurrence = when (item.recurrence) {
                "weekly" -> "每周"
                "monthly" -> "每月"
                "yearly" -> "每年"
                else -> ""
            }
            binding.billDetail.text = listOf(item.category, recurrence, item.note).filter(String::isNotBlank).joinToString(" · ")
            val sign = if (item.direction == "income") "+" else "−"
            binding.billAmount.text = sign + BillAmounts.format(item.amountMinor, item.currency)
            val directionColor = if (item.direction == "income") R.color.primary else R.color.danger
            binding.billDirectionMark.setBackgroundColor(ContextCompat.getColor(context, directionColor))
            binding.billAmount.setTextColor(ContextCompat.getColor(context, if (item.direction == "income") R.color.primary else R.color.text_primary))

            val pending = item.status == "pending"
            binding.billStatus.text = if (pending) "未入账" else "已入账"
            binding.billStatus.setTextColor(ContextCompat.getColor(context, if (pending) R.color.stock_low_text else R.color.stock_healthy_text))
            binding.billStatus.setBackgroundResource(if (pending) R.drawable.bg_stock_low else R.drawable.bg_stock_healthy)
            binding.billStatus.isClickable = pending
            binding.billStatus.isFocusable = pending
            binding.billStatus.contentDescription = if (pending) "将 ${item.title} 标记为已入账" else "${item.title} 已入账"
            binding.billStatus.setOnClickListener { if (pending) onMarkPosted(item) }

            binding.billCard.setOnClickListener { onEdit(item) }
            binding.billDelete.setOnClickListener { onDelete(item) }
        }
    }

    private fun dateKey(record: BillRecord): String =
        Instant.ofEpochMilli(record.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
