package com.kfaino.diapertracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemManageCategoryBinding

class CategoryManageAdapter(
    private val dataStore: DataStore,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onDelete: (Int, String) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.VH>() {

    private var items: List<String> = emptyList()

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemManageCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = items[position]
        val isPreset = dataStore.isPresetCategory(cat)
        val b = holder.binding

        b.categoryName.text = cat
        if (isPreset) {
            b.categoryTag.text = "预设"
            b.categoryTag.setBackgroundResource(R.drawable.bg_tag_preset)
            b.categoryTag.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.tag_preset_text))
        } else {
            b.categoryTag.text = "自定义"
            b.categoryTag.setBackgroundResource(R.drawable.bg_tag_custom)
            b.categoryTag.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.tag_custom_text))
        }

        b.btnMoveUp.isEnabled = position > 0
        b.btnMoveUp.alpha = if (position > 0) 1.0f else 0.3f
        b.btnMoveUp.setOnClickListener { onMoveUp(position) }

        b.btnMoveDown.isEnabled = position < items.size - 1
        b.btnMoveDown.alpha = if (position < items.size - 1) 1.0f else 0.3f
        b.btnMoveDown.setOnClickListener { onMoveDown(position) }

        b.btnDelete.setOnClickListener { onDelete(position, cat) }
    }

    class VH(val binding: ItemManageCategoryBinding) : RecyclerView.ViewHolder(binding.root)
}
