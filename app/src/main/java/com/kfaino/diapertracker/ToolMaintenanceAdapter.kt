package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemToolRecordBinding
import java.util.Date

/**
 * 🔧 家庭工具、五金配件与设备维保列表适配器 (Tool Maintenance Adapter)
 */
class ToolMaintenanceAdapter(
    private val activity: Activity,
    private val list: List<ToolMaintenanceRecord>,
    private val onMaintainClick: (ToolMaintenanceRecord) -> Unit,
    private val onEditClick: (ToolMaintenanceRecord) -> Unit,
    private val onDeleteClick: (ToolMaintenanceRecord) -> Unit
) : RecyclerView.Adapter<ToolMaintenanceAdapter.ToolViewHolder>() {

    class ToolViewHolder(val binding: ItemToolRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val record = list[position]
        val binding = holder.binding

        val qtyStr = if (record.qty % 1.0 == 0.0) record.qty.toInt().toString() else record.qty.toString()
        binding.itemToolName.text = "${record.name} × $qtyStr ${record.unit}"
        binding.itemToolCategoryBadge.text = record.getCategoryDisplayName()

        // 维保状态与预警
        if (record.isMaintenanceDue()) {
            binding.itemToolStatusBadge.visibility = View.VISIBLE
            binding.itemToolStatusBadge.text = "🔴 维保已逾期 (需及时更换)"
            binding.itemToolStatusBadge.setTextColor(activity.getColor(R.color.danger))
        } else if (record.isMaintenanceDueSoon()) {
            binding.itemToolStatusBadge.visibility = View.VISIBLE
            binding.itemToolStatusBadge.text = "🟡 15天内需维保"
            binding.itemToolStatusBadge.setTextColor(activity.getColor(R.color.accent_dark))
        } else {
            binding.itemToolStatusBadge.visibility = View.GONE
        }

        // 规格型号 / 搭配钻头
        if (record.spec.isNotBlank()) {
            binding.itemToolSpec.visibility = View.VISIBLE
            binding.itemToolSpec.text = "🔩 规格参数: ${record.spec}"
        } else {
            binding.itemToolSpec.visibility = View.GONE
        }

        // 存放位置
        val locStr = if (record.location.isNotBlank()) record.location else "未标注收纳位"
        binding.itemToolLocation.text = "📍 存放位置: $locStr"

        // 维保排期与时效
        if (record.maintenanceIntervalDays > 0) {
            val nextDate = record.getNextMaintenanceDate()
            val nextStr = if (nextDate > 0L) VaultUiHelper.standardDateFormat.format(Date(nextDate)) else "待初次维保打卡"
            val lastStr = if (record.lastMaintainedAt > 0L) " · 最近维保: ${VaultUiHelper.standardDateFormat.format(Date(record.lastMaintainedAt))}" else ""
            binding.itemToolMaintenanceText.text = "🛠️ 维保周期: 每 ${record.maintenanceIntervalDays} 天 · 下次排期: $nextStr$lastStr"
            binding.btnItemMaintainTool.visibility = View.VISIBLE
        } else {
            binding.itemToolMaintenanceText.text = "🛠️ 维保属性: 长期工具/五金 · 无需周期更换"
            binding.btnItemMaintainTool.visibility = View.GONE
        }

        // 备忘要领
        if (record.notes.isNotBlank()) {
            binding.itemToolNotes.visibility = View.VISIBLE
            binding.itemToolNotes.text = "📝 备忘: ${record.notes}"
        } else {
            binding.itemToolNotes.visibility = View.GONE
        }

        // 交互与动效
        binding.btnItemMaintainTool.applyPressScaleAnimation(0.92f)
        binding.btnItemMaintainTool.setOnClickListener { onMaintainClick(record) }

        binding.btnItemEditTool.applyPressScaleAnimation(0.90f)
        binding.btnItemEditTool.setOnClickListener { onEditClick(record) }

        binding.btnItemDeleteTool.applyPressScaleAnimation(0.90f)
        binding.btnItemDeleteTool.setOnClickListener { onDeleteClick(record) }
    }
}
