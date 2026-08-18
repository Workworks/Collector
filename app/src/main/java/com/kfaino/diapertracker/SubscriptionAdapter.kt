package com.kfaino.diapertracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemSubCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionAdapter(
    private val onSubClick: (Entry) -> Unit,
    private val onMoreClick: (Entry, View) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    fun submitList(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSubCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sub = items[position]
        val b = holder.binding

        holder.itemView.applyPressScaleAnimation(0.96f)
        holder.itemView.setOnClickListener { onSubClick(sub) }
        b.btnSubMore.setOnClickListener { onMoreClick(sub, it) }

        b.subName.text = sub.brand
        b.subCycleBadge.text = "🔄 ${sub.subCycle}${if (sub.subAutoRenew) "自动续费" else "单次"}"
        b.subPrice.text = "¥${String.format(Locale.getDefault(), "%.2f", sub.price)}"
        b.subPriceCycleLabel.text = "/${sub.subCycle.replace("按", "")}"

        val days = sub.getDaysOwned()
        b.subDaysText.text = "已订阅 $days 天"

        if (sub.subNextBillingDate > 0) {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = df.format(Date(sub.subNextBillingDate))
            val daysUntil = ((sub.subNextBillingDate - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt()
            val remainText = if (daysUntil >= 0) " (还有${daysUntil}天)" else " (已逾期)"
            b.subNextBillingText.text = "下次扣费: $dateStr$remainText"
        } else {
            b.subNextBillingText.text = "下次扣费: 未设定"
        }
    }

    class VH(val binding: ItemSubCardBinding) : RecyclerView.ViewHolder(binding.root)
}
