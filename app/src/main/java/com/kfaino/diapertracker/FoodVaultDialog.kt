package com.kfaino.diapertracker

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogFoodVaultBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 🥦 冰箱冷冻与食材生鲜鲜度库控制器 (Food & Fresh Vault Dialog)
 * - 支持冷冻室、冷藏室、干货调料、常温果蔬温区分区
 * - 智能计算已过期红牌、3天临期急需消灭预警与开封倒计时
 * - 🍳「今晚清库存」一键筛选、一键消耗 -1 份与开封打卡
 */
object FoodVaultDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogFoodVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentZoneFilter = "all" // "all", "expiring", "freezer", "fridge", "pantry", "room"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allFoods = store.getFoods().filter { !it.isConsumed }
            val expiringCount = allFoods.count { it.isExpiringSoon() }
            val expiredCount = allFoods.count { it.isExpired() }

            binding.tvStatTotalFoods.text = "${allFoods.size}"
            binding.tvStatExpiringFoods.text = "$expiringCount"
            binding.tvStatExpiredFoods.text = "$expiredCount"

            val filtered = allFoods.filter { food ->
                val matchesZone = when (currentZoneFilter) {
                    "all" -> true
                    "expiring" -> food.isExpiringSoon() || food.isExpired()
                    else -> food.zone == currentZoneFilter
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        food.name.contains(currentSearchKeyword, ignoreCase = true) ||
                        food.location.contains(currentSearchKeyword, ignoreCase = true) ||
                        food.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesZone && matchesSearch
            }.sortedWith(
                compareBy<FoodRecord> {
                    if (it.isExpired()) 0 else if (it.isExpiringSoon()) 1 else 2
                }.thenBy {
                    if (it.getEffectiveExpiryDate() > 0L) it.getEffectiveExpiryDate() else Long.MAX_VALUE
                }
            )

            binding.layoutFoodEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvFoods.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvFoods.adapter = FoodAdapter(activity, filtered,
                onConsumeClick = { food ->
                    val qtyInt = if (food.qty % 1.0 == 0.0) food.qty.toInt().toString() else food.qty.toString()
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "消耗/烹饪食材",
                        message = "确认消耗 1 份【${food.name}】？\n当前剩余: $qtyInt ${food.unit}",
                        emoji = "🍳",
                        positiveText = "消耗 1 份",
                        negativeText = "取消"
                    ) {
                        store.consumeFood(food.id, 1.0)
                        Toast.makeText(activity, "🎉 已消耗 1 份【${food.name}】", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                },
                onOpenClick = { food ->
                    store.markFoodOpened(food.id)
                    Toast.makeText(activity, "✨ 已标记【${food.name}】开封", Toast.LENGTH_SHORT).show()
                    reloadList()
                    onDataChanged()
                },
                onItemClick = { food ->
                    showAddOrEditFoodDialog(activity, store, food) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { food ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出食材",
                        message = "确认将【${food.name}】彻底移出食材库？",
                        emoji = "🗑️",
                        positiveText = "移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteFood(food.id)
                        Toast.makeText(activity, "已移出食材记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvFoods.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupFoodZones.setOnCheckedStateChangeListener { _, checkedIds ->
            currentZoneFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_zone_expiring -> "expiring"
                R.id.chip_zone_freezer -> "freezer"
                R.id.chip_zone_fridge -> "fridge"
                R.id.chip_zone_pantry -> "pantry"
                R.id.chip_zone_room -> "room"
                else -> "all"
            }
            reloadList()
        }
        VaultUiHelper.bindSearchWatcher(binding.etSearchFood) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddFood.applyPressScaleAnimation(0.92f)
        binding.btnAddFood.setOnClickListener {
            showAddOrEditFoodDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseFoodVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseFoodVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出新增或编辑食材生鲜弹窗 */
    fun showAddOrEditFoodDialog(
        activity: Activity,
        store: DataStore,
        editingFood: FoodRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_food, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_title)
        val etName = view.findViewById<EditText>(R.id.et_food_name)
        val chipGroupZone = view.findViewById<ChipGroup>(R.id.chip_group_food_zone_edit)
        val etQty = view.findViewById<EditText>(R.id.et_food_qty)
        val etUnit = view.findViewById<EditText>(R.id.et_food_unit)
        val etLocation = view.findViewById<EditText>(R.id.et_food_location)
        val btnPickExpiry = view.findViewById<View>(R.id.btn_pick_food_expiry)
        val tvExpiryText = view.findViewById<TextView>(R.id.tv_food_expiry_text)
        val cbIsOpened = view.findViewById<CheckBox>(R.id.cb_food_is_opened)
        val etOpenedKeepDays = view.findViewById<EditText>(R.id.et_opened_keep_days)
        val etNotes = view.findViewById<EditText>(R.id.et_food_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_food)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_food)

        var selectedExpiryDate = editingFood?.expiryDate ?: 0L

        fun updateExpiryUi() {
            if (selectedExpiryDate > 0L) {
                tvExpiryText.text = "📅 保质截止: ${VaultUiHelper.standardDateFormat.format(Date(selectedExpiryDate))}"
                tvExpiryText.setTextColor(activity.getColor(R.color.primary))
            } else {
                tvExpiryText.text = "📅 设为长期在库 / 暂不设限"
                tvExpiryText.setTextColor(activity.getColor(R.color.text_secondary))
            }
        }

        if (editingFood != null) {
            tvTitle.text = "🥦 编辑食材保鲜记录"
            etName.setText(editingFood.name)
            when (editingFood.zone) {
                "fridge" -> chipGroupZone.check(R.id.chip_edit_fridge)
                "pantry" -> chipGroupZone.check(R.id.chip_edit_pantry)
                "room" -> chipGroupZone.check(R.id.chip_edit_room)
                else -> chipGroupZone.check(R.id.chip_edit_freezer)
            }
            etQty.setText(if (editingFood.qty % 1.0 == 0.0) editingFood.qty.toInt().toString() else editingFood.qty.toString())
            etUnit.setText(editingFood.unit)
            etLocation.setText(editingFood.location)
            cbIsOpened.isChecked = editingFood.isOpened
            if (editingFood.openedValidityDays > 0) {
                etOpenedKeepDays.setText("${editingFood.openedValidityDays}")
            }
            etNotes.setText(editingFood.notes)
            updateExpiryUi()
        } else {
            tvTitle.text = "🥦 放入新食材"
            chipGroupZone.check(R.id.chip_edit_freezer)
            updateExpiryUi()
        }

        btnPickExpiry.setOnClickListener {
            VaultUiHelper.showDatePicker(activity, selectedExpiryDate) { time, _ ->
                selectedExpiryDate = time
                updateExpiryUi()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "请输入食材名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zone = when (chipGroupZone.checkedChipId) {
                R.id.chip_edit_fridge -> "fridge"
                R.id.chip_edit_pantry -> "pantry"
                R.id.chip_edit_room -> "room"
                else -> "freezer"
            }
            val qty = etQty.text.toString().toDoubleOrNull() ?: 1.0
            val unit = etUnit.text.toString().trim().ifEmpty { "份" }
            val location = etLocation.text.toString().trim().ifEmpty { "冰箱储藏区" }
            val isOpened = cbIsOpened.isChecked
            val openedDays = etOpenedKeepDays.text.toString().toIntOrNull() ?: 0
            val notes = etNotes.text.toString().trim()

            val openedAtTime = if (isOpened) {
                if (editingFood != null && editingFood.isOpened && editingFood.openedAt > 0L) editingFood.openedAt else System.currentTimeMillis()
            } else 0L

            val food = FoodRecord(
                id = editingFood?.id ?: java.util.UUID.randomUUID().toString(),
                name = name,
                zone = zone,
                qty = qty,
                unit = unit,
                location = location,
                purchaseDate = editingFood?.purchaseDate ?: System.currentTimeMillis(),
                expiryDate = selectedExpiryDate,
                isOpened = isOpened,
                openedAt = openedAtTime,
                openedValidityDays = openedDays,
                notes = notes,
                isConsumed = false
            )

            store.addOrUpdateFood(food)
            Toast.makeText(activity, "🎉 已将【$name】存入鲜度库！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }

    private class FoodAdapter(
        private val activity: Activity,
        private val list: List<FoodRecord>,
        private val onConsumeClick: (FoodRecord) -> Unit,
        private val onOpenClick: (FoodRecord) -> Unit,
        private val onItemClick: (FoodRecord) -> Unit,
        private val onDeleteClick: (FoodRecord) -> Unit
    ) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

        class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.item_food_name)
            val tvZoneBadge: TextView = itemView.findViewById(R.id.item_food_zone_badge)
            val tvFreshnessBadge: TextView = itemView.findViewById(R.id.item_food_freshness_badge)
            val tvLocationQty: TextView = itemView.findViewById(R.id.item_food_location_qty)
            val tvOpenedStatus: TextView = itemView.findViewById(R.id.item_food_opened_status)
            val tvNotes: TextView = itemView.findViewById(R.id.item_food_notes)
            val btnConsume: View = itemView.findViewById(R.id.btn_item_consume_food)
            val btnOpen: View = itemView.findViewById(R.id.btn_item_open_food)
            val btnDelete: View = itemView.findViewById(R.id.btn_item_delete_food)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food_record, parent, false)
            return FoodViewHolder(view)
        }

        override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
            val food = list[position]
            holder.tvName.text = food.name
            holder.tvZoneBadge.text = food.getZoneDisplayName()
            holder.tvFreshnessBadge.text = food.getFreshnessStatusText()

            if (food.isExpired()) {
                holder.tvFreshnessBadge.setTextColor(Color.parseColor("#EF4444"))
                holder.tvFreshnessBadge.setBackgroundResource(R.drawable.bg_chip_inactive)
            } else if (food.isExpiringSoon()) {
                holder.tvFreshnessBadge.setTextColor(Color.parseColor("#F59E0B"))
                holder.tvFreshnessBadge.setBackgroundResource(R.drawable.bg_chip_inactive)
            } else {
                holder.tvFreshnessBadge.setTextColor(Color.parseColor("#10B981"))
                holder.tvFreshnessBadge.setBackgroundResource(R.drawable.bg_chip_inactive)
            }

            val qtyStr = if (food.qty % 1.0 == 0.0) food.qty.toInt().toString() else food.qty.toString()
            holder.tvLocationQty.text = "📍 ${food.location}  ·  📦 存量: $qtyStr ${food.unit}"

            if (food.isOpened) {
                holder.tvOpenedStatus.visibility = View.VISIBLE
                val openDays = ((System.currentTimeMillis() - food.openedAt) / (24L * 60 * 60 * 1000)).toInt()
                val suggestStr = if (food.openedValidityDays > 0) " (建议 ${food.openedValidityDays} 天内)" else ""
                holder.tvOpenedStatus.text = "⚡ 已开封 ${openDays}天$suggestStr"
                holder.btnOpen.visibility = View.GONE
            } else {
                holder.tvOpenedStatus.visibility = View.GONE
                holder.btnOpen.visibility = View.VISIBLE
            }

            if (food.notes.isNotEmpty()) {
                holder.tvNotes.visibility = View.VISIBLE
                holder.tvNotes.text = "💡 ${food.notes}"
            } else {
                holder.tvNotes.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onItemClick(food) }
            holder.btnConsume.setOnClickListener { onConsumeClick(food) }
            holder.btnOpen.setOnClickListener { onOpenClick(food) }
            holder.btnDelete.setOnClickListener { onDeleteClick(food) }
        }

        override fun getItemCount(): Int = list.size
    }
}
