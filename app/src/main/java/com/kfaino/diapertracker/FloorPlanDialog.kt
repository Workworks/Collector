package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogFloorPlanPickerBinding

object FloorPlanDialog {

    /**
     * 弹出平面图选点或查看房间物品弹窗
     */
    fun show(
        activity: Activity,
        store: DataStore,
        isSelectMode: Boolean = true,
        currentHouseName: String? = null,
        onLocationSelected: ((houseName: String, roomName: String, pinX: Float, pinY: Float) -> Unit)? = null
    ) {
        val binding = DialogFloorPlanPickerBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        var houses = store.getHouses()
        var selectedHouse = houses.find { it.name == currentHouseName } ?: houses.firstOrNull() ?: store.addHouse("🏠 自己的家")

        var selectedRoomName = ""
        var selectedPx = -1f
        var selectedPy = -1f

        binding.interactiveFloorPlan.isPinSelectionMode = isSelectMode
        binding.interactiveFloorPlan.houseSpace = selectedHouse
        binding.interactiveFloorPlan.entries = store.loadAll()

        fun refreshHouseTabs() {
            binding.houseTabsContainer.removeAllViews()
            for (h in houses) {
                val chip = TextView(activity).apply {
                    text = h.name
                    textSize = 12f
                    gravity = Gravity.CENTER
                    val isSelected = (h.id == selectedHouse.id)
                    setPadding(activity.dpToPx(12), activity.dpToPx(6), activity.dpToPx(12), activity.dpToPx(6))
                    if (isSelected) {
                        setBackgroundResource(R.drawable.bg_chip_active)
                        setTextColor(Color.WHITE)
                    } else {
                        setBackgroundResource(R.drawable.bg_chip_inactive)
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }
                    applyPressScaleAnimation(0.92f)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = activity.dpToPx(6) }
                    layoutParams = params

                    setOnClickListener {
                        selectedHouse = h
                        binding.interactiveFloorPlan.houseSpace = h
                        binding.interactiveFloorPlan.invalidate()
                        refreshHouseTabs()
                    }
                }
                binding.houseTabsContainer.addView(chip)
            }

            // + 新增空间
            val addChip = TextView(activity).apply {
                text = "+ 新增家/空间"
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(activity.dpToPx(10), activity.dpToPx(6), activity.dpToPx(10), activity.dpToPx(6))
                setBackgroundResource(R.drawable.bg_btn_custom_add)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                applyPressScaleAnimation(0.92f)
                setOnClickListener {
                    showAddHouseDialog(activity, store) { newH ->
                        houses = store.getHouses()
                        selectedHouse = newH
                        binding.interactiveFloorPlan.houseSpace = newH
                        binding.interactiveFloorPlan.invalidate()
                        refreshHouseTabs()
                    }
                }
            }
            binding.houseTabsContainer.addView(addChip)
        }

        refreshHouseTabs()

        binding.interactiveFloorPlan.onPinPlaced = { px, py, rName ->
            selectedPx = px
            selectedPy = py
            selectedRoomName = rName
            binding.selectedRoomInfo.text = "📍 标记位置：${selectedHouse.name} · 【$rName】"
        }

        binding.interactiveFloorPlan.onRoomClicked = { room, items ->
            val summary = if (items.isNotEmpty()) {
                val names = items.map { "${it.brand} (${it.qty}${it.unit})" }.joinToString("、")
                "【${room.name}】共 ${items.size} 种物品：$names"
            } else {
                "【${room.name}】暂无存放物品"
            }
            binding.selectedRoomInfo.text = "🏠 $summary"
        }

        binding.btnManageRoomsTop.applyPressScaleAnimation(0.92f)
        binding.btnManageRoomsTop.setOnClickListener {
            RoomManagerDialog.showManageDialog(activity, store, selectedHouse) { updatedHouse ->
                selectedHouse = updatedHouse
                houses = store.getHouses()
                binding.interactiveFloorPlan.houseSpace = updatedHouse
                binding.interactiveFloorPlan.invalidate()
            }
        }

        binding.btnCloseFloorplan.applyPressScaleAnimation(0.92f)
        binding.btnConfirmFloorplan.applyPressScaleAnimation(0.92f)

        binding.btnCloseFloorplan.setOnClickListener { dialog.dismiss() }
        binding.btnConfirmFloorplan.setOnClickListener {
            if (selectedRoomName.isNotBlank() || selectedPx >= 0f) {
                onLocationSelected?.invoke(selectedHouse.name, selectedRoomName, selectedPx, selectedPy)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddHouseDialog(activity: Activity, store: DataStore, onCreated: (HouseSpace) -> Unit) {
        val input = EditText(activity).apply {
            hint = "如：🏡 父母家、🏢 公司办公室、🚗 汽车后备箱"
            setPadding(40, 30, 40, 30)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#94A3B8"))
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("新增空间/家庭")
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newH = store.addHouse(name)
                    Toast.makeText(activity, "已成功创建新空间: $name", Toast.LENGTH_SHORT).show()
                    onCreated(newH)
                }
            }
            .show()
    }

    private fun Activity.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
