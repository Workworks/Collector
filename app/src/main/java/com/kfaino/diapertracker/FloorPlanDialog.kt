package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogFloorPlanPickerBinding
import java.util.Locale

/**
 * 🗺️ 空间平面图与箱盒 X 光透视舱 (Floor Plan & Box X-Ray Hub)
 * 1. 可视化多房间平面图网格与触控选点
 * 2. 物品穿梭定位与高亮打点
 * 3. 📦 箱盒 X 光透视舱：微观资产估值、总件数与空间负荷指数
 * 4. 🚚 一键整箱搬家与批量流转：整箱跨房间批量挪移与全量变迁轨迹追踪
 */
object FloorPlanDialog {

    private const val TAG = "FloorPlanDialog"

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
        var allEntries = store.loadAll()
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
            binding.floorplanHint.text = "👉 点击任意房间可展开【箱盒 X 光透视舱】与整箱搬家"
            binding.btnConfirmFloorplan.text = "我知道了"
        } else {
            binding.btnConfirmFloorplan.text = "确认此位置标记"
        }

        fun showBatchRelocateDialog(currentHouse: HouseSpace, currentRoom: String, itemsToMove: List<Entry>, onRelocated: () -> Unit) {
            val allHouses = store.getHouses()
            if (allHouses.isEmpty()) return

            val houseOptions = allHouses.map { it.name }
            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择搬迁目标空间",
                emoji = "🚚",
                options = houseOptions,
                selectedIndex = allHouses.indexOfFirst { it.id == currentHouse.id }.coerceAtLeast(0)
            ) { houseIdx, _ ->
                val targetHouse = allHouses[houseIdx]
                val targetRooms = targetHouse.rooms
                if (targetRooms.isEmpty()) {
                    Toast.makeText(activity, "目标空间【${targetHouse.name}】下暂无房间，请先添加房间！", Toast.LENGTH_LONG).show()
                    return@showSingleChoiceDialog
                }

                val roomOptions = targetRooms.map { it.name }
                ModernDialogHelper.showSingleChoiceDialog(
                    context = activity,
                    title = "选择目标收纳房间/区域",
                    emoji = "📦",
                    options = roomOptions,
                    selectedIndex = 0
                ) { roomIdx, _ ->
                    val targetRoom = targetRooms[roomIdx]
                    if (targetHouse.name == currentHouse.name && targetRoom.name == currentRoom) {
                        Toast.makeText(activity, "目标位置与当前位置相同，无需移动", Toast.LENGTH_SHORT).show()
                        return@showSingleChoiceDialog
                    }

                    // 批量更新物品
                    val all = store.loadAll().toMutableList()
                    var moveCount = 0
                    val now = System.currentTimeMillis()
                    for (i in 0 until all.size) {
                        val e = all[i]
                        if (itemsToMove.any { it.id == e.id }) {
                            val newHist = e.locationHistory.toMutableList()
                            newHist.add(
                                0,
                                LocationMovement(
                                    location = "【${targetRoom.name}】",
                                    houseName = targetHouse.name,
                                    roomName = targetRoom.name,
                                    movedAt = now,
                                    note = "🚚 从【${currentHouse.name} / $currentRoom】整箱搬家流转"
                                )
                            )
                            all[i] = e.copy(
                                houseName = targetHouse.name,
                                roomName = targetRoom.name,
                                location = targetRoom.name,
                                locationHistory = newHist
                            )
                            moveCount++
                        }
                    }

                    store.saveAll(all)
                    Toast.makeText(activity, "🎉 成功将 $moveCount 件物品整箱搬迁至【${targetHouse.name} · ${targetRoom.name}】！", Toast.LENGTH_LONG).show()
                    onRelocated()
                }
            }
        }

        fun showRoomItemsDrawer(roomName: String, items: List<Entry>) {
            binding.roomAssetsContainer.removeAllViews()
            if (items.isEmpty()) {
                binding.roomAssetsScroll.visibility = View.GONE
                return
            }

            binding.roomAssetsScroll.visibility = View.VISIBLE

            // 1. 头部标题与操作按钮栏 (便签工坊 / 整箱搬家)
            val headerRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 8)
            }

            val headerTv = TextView(activity).apply {
                text = "📦 【$roomName】微观收纳舱"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnRelocate = TextView(activity).apply {
                text = "🚚 整箱搬家"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.accent_dark))
                setBackgroundResource(R.drawable.bg_btn_custom_add)
                setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                applyPressScaleAnimation(0.92f)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = activity.dpToPx(6) }
                layoutParams = params
                setOnClickListener {
                    showBatchRelocateDialog(selectedHouse, roomName, items) {
                        allEntries = store.loadAll()
                        binding.interactiveFloorPlan.entries = allEntries
                        binding.interactiveFloorPlan.invalidate()
                        val updatedItems = allEntries.filter { it.roomName == roomName && !it.isRetired }
                        showRoomItemsDrawer(roomName, updatedItems)
                    }
                }
            }

            val btnGenQr = TextView(activity).apply {
                text = "🏷️ 便签工坊"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                setBackgroundResource(R.drawable.bg_btn_custom_add)
                setPadding(activity.dpToPx(8), activity.dpToPx(4), activity.dpToPx(8), activity.dpToPx(4))
                applyPressScaleAnimation(0.92f)
                setOnClickListener {
                    BoxQrCodeDialog.show(activity, store, selectedHouse.name, roomName)
                }
            }

            headerRow.addView(headerTv)
            headerRow.addView(btnRelocate)
            headerRow.addView(btnGenQr)
            binding.roomAssetsContainer.addView(headerRow)

            // 2. 箱盒 X-Ray 透视指标卡片 (资产估值、总件数与空间负荷评估)
            val totalVal = items.sumOf { it.price * it.qty }
            val totalQty = items.sumOf { it.qty }
            val densityRating = if (totalQty <= 5) "🟢 宽敞轻量" else if (totalQty <= 15) "🟡 适宜收纳" else "🟠 密集收纳"
            val catList = items.map { it.category }.distinct().joinToString("、")

            val xrayCard = MaterialCardView(activity).apply {
                radius = activity.dpToPx(12).toFloat()
                cardElevation = 0f
                strokeWidth = activity.dpToPx(1)
                setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dpToPx(8) }
            }

            val xrayLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dpToPx(12), activity.dpToPx(10), activity.dpToPx(12), activity.dpToPx(10))
            }

            val metricRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val valTv = TextView(activity).apply {
                text = "💰 总值: ¥${String.format(Locale.getDefault(), "%.2f", totalVal)}"
                textSize = 12f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val densityTv = TextView(activity).apply {
                text = "$densityRating ($totalQty 件)"
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            metricRow.addView(valTv)
            metricRow.addView(densityTv)
            xrayLayout.addView(metricRow)

            if (catList.isNotBlank()) {
                val catTv = TextView(activity).apply {
                    text = "🏷️ 分类构成: $catList"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setPadding(0, activity.dpToPx(3), 0, 0)
                }
                xrayLayout.addView(catTv)
            }

            xrayCard.addView(xrayLayout)
            binding.roomAssetsContainer.addView(xrayCard)

            // 3. 物品明细卡片列表
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
        ModernDialogHelper.showInputDialog(
            context = activity,
            title = "新增空间/场所",
            subtitle = "输入家庭或办公场所名称",
            hint = "如：🏡 父母家、🏢 办公室、🚗 后备箱",
            emoji = "🏡"
        ) { name ->
            if (name.isNotEmpty()) {
                val newH = store.addHouse(name)
                Toast.makeText(activity, "已成功创建新空间: $name", Toast.LENGTH_SHORT).show()
                onCreated(newH)
            }
        }
    }

    private fun Activity.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
