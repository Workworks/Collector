package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogFloorPlanPickerBinding
import java.util.Locale

object FloorPlanDialog {

    /**
     * 弹出平面图选点、查看房间物品或从物品一键穿梭高亮
     */
    fun show(
        activity: Activity,
        store: DataStore,
        isSelectMode: Boolean = true,
        currentHouseName: String? = null,
        targetEntry: Entry? = null,
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
        val defaultHouseName = targetEntry?.houseName ?: currentHouseName
        var selectedHouse = houses.find { it.name == defaultHouseName } ?: houses.firstOrNull() ?: store.addHouse("🏠 自己的家")

        var selectedRoomName = targetEntry?.roomName ?: ""
        var selectedPx = targetEntry?.pinX ?: -1f
        var selectedPy = targetEntry?.pinY ?: -1f

        binding.interactiveFloorPlan.isPinSelectionMode = isSelectMode
        binding.interactiveFloorPlan.houseSpace = selectedHouse
        val allEntries = store.loadAll()
        binding.interactiveFloorPlan.entries = allEntries

        // 若是由特定物品穿梭进入
        if (targetEntry != null) {
            binding.floorplanTitle.text = "🎯 物品位置穿梭定位"
            binding.floorplanHint.text = "正在定位【${targetEntry.brand}】所在空间与房间"
            binding.interactiveFloorPlan.highlightedRoomName = targetEntry.roomName
            binding.interactiveFloorPlan.highlightedPinX = targetEntry.pinX
            binding.interactiveFloorPlan.highlightedPinY = targetEntry.pinY
            binding.selectedRoomInfo.text = "📍 ${targetEntry.houseName} · 【${targetEntry.roomName.ifEmpty { "房间未设" }}】 ${targetEntry.location}"
            binding.btnConfirmFloorplan.text = "关闭定位"
        } else if (!isSelectMode) {
            binding.floorplanTitle.text = "🗺️ 家庭空间平面图全景"
            binding.floorplanHint.text = "👉 点击任意房间可展开查看该房间内的所有在库物品"
            binding.btnConfirmFloorplan.text = "我知道了"
        } else {
            binding.btnConfirmFloorplan.text = "确认此位置标记"
        }

        fun showRoomItemsDrawer(roomName: String, items: List<Entry>) {
            binding.roomAssetsContainer.removeAllViews()
            if (items.isEmpty()) {
                binding.roomAssetsScroll.visibility = View.GONE
                return
            }

            binding.roomAssetsScroll.visibility = View.VISIBLE

            // 头部标题
            val headerTv = TextView(activity).apply {
                text = "📦 【$roomName】存放的在库物品 (共 ${items.size} 种):"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                paint.isFakeBoldText = true
                setPadding(0, 4, 0, 8)
            }
            binding.roomAssetsContainer.addView(headerTv)

            for (item in items) {
                val card = MaterialCardView(activity).apply {
                    radius = activity.dpToPx(10).toFloat()
                    cardElevation = 0f
                    strokeWidth = activity.dpToPx(1)
                    setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.input_bg))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = activity.dpToPx(6) }
                }

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(activity.dpToPx(10), activity.dpToPx(8), activity.dpToPx(10), activity.dpToPx(8))
                }

                // 缩略图或 Emoji
                val iconView: View = if (item.photoPath.isNotBlank()) {
                    val bm = ImageVaultHelper.loadSampledBitmap(activity, item.photoPath, 80, 80)
                    if (bm != null) {
                        ImageView(activity).apply {
                            setImageBitmap(bm)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams = LinearLayout.LayoutParams(activity.dpToPx(28), activity.dpToPx(28)).apply {
                                marginEnd = activity.dpToPx(8)
                            }
                        }
                    } else {
                        TextView(activity).apply {
                            text = "📦"
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                marginEnd = activity.dpToPx(8)
                            }
                        }
                    }
                } else {
                    TextView(activity).apply {
                        text = "📦"
                        textSize = 16f
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = activity.dpToPx(8)
                        }
                    }
                }

                val titleTv = TextView(activity).apply {
                    text = item.brand
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    paint.isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val priceTv = TextView(activity).apply {
                    text = "¥${String.format(Locale.getDefault(), "%.2f", item.price * item.qty)}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setPadding(0, 0, activity.dpToPx(8), 0)
                }

                val qtyBadge = TextView(activity).apply {
                    text = "${item.qty} ${item.unit}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(context, R.color.stock_healthy_text))
                    setBackgroundResource(R.drawable.bg_stock_healthy)
                    setPadding(activity.dpToPx(6), activity.dpToPx(2), activity.dpToPx(6), activity.dpToPx(2))
                }

                row.addView(iconView)
                row.addView(titleTv)
                row.addView(priceTv)
                row.addView(qtyBadge)
                card.addView(row)

                card.applyPressScaleAnimation(0.95f)
                card.setOnClickListener {
                    dialog.dismiss()
                    (activity as? MainActivity)?.showEditDialog(item)
                }

                binding.roomAssetsContainer.addView(card)
            }
        }

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

        // 初始若有 targetEntry 或已选房间，展示该房间物品抽屉
        if (targetEntry != null && targetEntry.roomName.isNotBlank()) {
            val roomItems = allEntries.filter { it.roomName == targetEntry.roomName && !it.isRetired }
            showRoomItemsDrawer(targetEntry.roomName, roomItems)
        }

        binding.interactiveFloorPlan.onPinPlaced = { px, py, rName ->
            selectedPx = px
            selectedPy = py
            selectedRoomName = rName
            binding.selectedRoomInfo.text = "📍 标记位置：${selectedHouse.name} · 【$rName】"
        }

        binding.interactiveFloorPlan.onRoomClicked = { room, items ->
            selectedRoomName = room.name
            binding.interactiveFloorPlan.highlightedRoomName = room.name
            binding.interactiveFloorPlan.invalidate()
            val activeItems = items.filter { !it.isRetired }
            val summary = if (activeItems.isNotEmpty()) {
                val names = activeItems.map { "${it.brand} (${it.qty}${it.unit})" }.joinToString("、")
                "【${room.name}】共 ${activeItems.size} 种物品：$names"
            } else {
                "【${room.name}】暂无存放物品"
            }
            binding.selectedRoomInfo.text = "🏠 $summary"
            showRoomItemsDrawer(room.name, activeItems)
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
            if (isSelectMode && (selectedRoomName.isNotBlank() || selectedPx >= 0f)) {
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
