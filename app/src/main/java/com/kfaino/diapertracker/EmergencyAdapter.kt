package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemEmergencyRecordBinding
import java.util.Date

/**
 * 🚨 家庭应急防灾物资列表适配器 (Emergency Adapter)
 */
class EmergencyAdapter(
    private val activity: Activity,
    private val list: List<EmergencyItem>,
    private val onCheckClick: (EmergencyItem) -> Unit,
    private val onEditClick: (EmergencyItem) -> Unit,
    private val onDeleteClick: (EmergencyItem) -> Unit
) : RecyclerView.Adapter<EmergencyAdapter.EmergencyViewHolder>() {

    class EmergencyViewHolder(val binding: ItemEmergencyRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyViewHolder {
        val binding = ItemEmergencyRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmergencyViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: EmergencyViewHolder, position: Int) {
        val item = list[position]
        val binding = holder.binding

        val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
        binding.itemEmergencyName.text = "${item.name} × $qtyStr ${item.unit}"
        binding.itemEmergencyKitBadge.text = item.getKitTypeDisplayName()
        binding.itemEmergencyCategoryBadge.text = item.getCategoryDisplayName()

        // 状态与失效预警
        if (item.isExpired()) {
            binding.itemEmergencyStatusBadge.visibility = View.VISIBLE
            binding.itemEmergencyStatusBadge.text = "🔴 已失效过期 (需立即更换)"
            binding.itemEmergencyStatusBadge.setTextColor(activity.getColor(R.color.danger))
        } else if (item.isExpiringSoon()) {
            binding.itemEmergencyStatusBadge.visibility = View.VISIBLE
            binding.itemEmergencyStatusBadge.text = "🟡 30天内临期失效"
            binding.itemEmergencyStatusBadge.setTextColor(activity.getColor(R.color.accent_dark))
        } else if (item.isNeedsCheck()) {
            binding.itemEmergencyStatusBadge.visibility = View.VISIBLE
            binding.itemEmergencyStatusBadge.text = "⏳ 待定期点检测试"
            binding.itemEmergencyStatusBadge.setTextColor(activity.getColor(R.color.accent_blue))
        } else {
            binding.itemEmergencyStatusBadge.visibility = View.GONE
        }

        // 位置 (黄金动线)
        val locStr = if (item.location.isNotBlank()) item.location else "未指定具体黄金动线位"
        binding.itemEmergencyLocation.text = "📍 存放位置: $locStr"

        // 时效说明
        val expStr = if (item.expiryDate > 0L) {
            "保质/失效至: ${VaultUiHelper.standardDateFormat.format(Date(item.expiryDate))}"
        } else {
            "长期耐用"
        }
        val rotStr = if (item.rotationIntervalMonths > 0) " · 建议每 ${item.rotationIntervalMonths} 个月测试" else ""
        val chkStr = if (item.lastCheckedAt > 0L) " · 最近点检: ${VaultUiHelper.standardDateFormat.format(Date(item.lastCheckedAt))}" else ""
        binding.itemEmergencyExpiryText.text = "📅 时效生命线: $expStr$rotStr$chkStr"

        // 备忘
        if (item.notes.isNotBlank()) {
            binding.itemEmergencyNotes.visibility = View.VISIBLE
            binding.itemEmergencyNotes.text = "📝 要领: ${item.notes}"
        } else {
            binding.itemEmergencyNotes.visibility = View.GONE
        }

        // 交互与动效
        binding.btnItemCheckEmergency.applyPressScaleAnimation(0.92f)
        binding.btnItemCheckEmergency.setOnClickListener { onCheckClick(item) }

        binding.btnItemEditEmergency.applyPressScaleAnimation(0.90f)
        binding.btnItemEditEmergency.setOnClickListener { onEditClick(item) }

        binding.btnItemDeleteEmergency.applyPressScaleAnimation(0.90f)
        binding.btnItemDeleteEmergency.setOnClickListener { onDeleteClick(item) }
    }
}
