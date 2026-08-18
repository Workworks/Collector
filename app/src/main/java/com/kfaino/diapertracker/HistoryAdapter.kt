package com.kfaino.diapertracker

import android.app.Activity
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
    private val onEdit: ((Entry, Int) -> Unit)? = null,
    private val onDelete: ((Entry, Int) -> Unit)? = null
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
        val unit = entry.unit.ifEmpty { "件" }

        if (entry.isRetired) {
            b.categoryTag.text = "🔴 已退役"
        } else if (entry.isSubscription) {
            b.categoryTag.text = "🔄 ${entry.subCycle}订阅"
        } else {
            b.categoryTag.text = entry.category
        }

        b.brand.text = (if (entry.isImportant) "⭐ " else "") + entry.brand
        b.time.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.ts))

        if (entry.isIn) {
            b.badge.text = "+"
            b.badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
            val totalAmount = entry.qty * entry.price
            b.amount.text = "+ ¥${String.format(Locale.getDefault(), "%.2f", totalAmount)}"
            b.amount.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            b.qtyInfo.text = if (entry.price > 0)
                "${entry.qty}$unit · 拥有 ${entry.getDaysOwned()} 天 · 日均 ¥${String.format(Locale.getDefault(), "%.2f", entry.getDailyCost())}"
            else
                "${entry.qty}$unit 入库 · 拥有 ${entry.getDaysOwned()} 天"
        } else {
            b.badge.text = "-"
            b.badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.danger))
            b.amount.text = "- ${entry.qty}$unit"
            b.amount.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
            b.qtyInfo.text = "出库消耗"
        }

        // 空间与位置信息绑定
        if (entry.location.isNotBlank()) {
            b.locationBadgeContainer.visibility = View.VISIBLE
            b.locationText.text = "📍 ${entry.houseName} · ${entry.location}"
            b.btnLocHistoryBadge.applyPressScaleAnimation(0.90f)
            b.btnLocHistoryBadge.setOnClickListener {
                if (ctx is Activity) {
                    LocationHistoryDialog.show(ctx, entry)
                }
            }
        } else {
            b.locationBadgeContainer.visibility = View.GONE
        }

        if (entry.notes.isNotBlank() || (entry.isRetired && entry.retiredAction.isNotBlank())) {
            b.notesText.visibility = View.VISIBLE
            val noteContent = if (entry.isRetired && entry.retiredAction.isNotBlank()) {
                "📦 退役归置: ${entry.retiredAction}${if (entry.notes.isNotBlank()) " · " + entry.notes else ""}"
            } else {
                "💬 备注: ${entry.notes}"
            }
            b.notesText.text = noteContent
        } else {
            b.notesText.visibility = View.GONE
        }

        // 点击编辑与删除
        holder.itemView.applyPressScaleAnimation(0.96f)
        b.editBtn.applyPressScaleAnimation(0.88f)
        b.deleteBtn.applyPressScaleAnimation(0.88f)

        holder.itemView.setOnClickListener { onEdit?.invoke(entry, position) }
        b.editBtn.setOnClickListener { onEdit?.invoke(entry, position) }
        b.deleteBtn.setOnClickListener { onDelete?.invoke(entry, position) }
    }

    class VH(val binding: RowHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}