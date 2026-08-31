package com.kfaino.diapertracker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogInputRoomBinding
import com.kfaino.diapertracker.databinding.DialogManageRoomsBinding
import java.util.UUID

object RoomManagerDialog {

    private val PRESET_ICONS = listOf(
        "🛋️", "🛏️", "🛌", "🍳", "🚪", "📦",
        "🌿", "📚", "🧸", "🛁", "🚗", "👗",
        "❄️", "🗄️", "🎮", "🍵"
    )

    private val PRESET_COLORS = listOf(
        "#10B981", "#3B82F6", "#8B5CF6", "#F59E0B",
        "#EC4899", "#06B6D4", "#EF4444", "#6366F1"
    )

    /** 弹出房间管理对话框 */
    fun showManageDialog(
        context: Context,
        store: DataStore,
        house: HouseSpace,
        onUpdated: (HouseSpace) -> Unit
    ) {
        val binding = DialogManageRoomsBinding.inflate(LayoutInflater.from(context))
        var currentHouse = house
        var rooms = currentHouse.rooms.toMutableList()

        lateinit var adapter: RoomManageAdapter

        fun refreshList() {
            rooms = currentHouse.rooms.toMutableList()
            adapter.submit(rooms.toList())
            onUpdated(currentHouse)
        }

        adapter = RoomManageAdapter(
            dataStore = store,
            houseSpace = currentHouse,
            onMoveUp = { pos ->
                if (pos > 0) {
                    val item = rooms.removeAt(pos)
                    rooms.add(pos - 1, item)
                    currentHouse = currentHouse.copy(rooms = rooms.toList())
                    store.updateHouse(currentHouse)
                    refreshList()
                }
            },
            onMoveDown = { pos ->
                if (pos < rooms.size - 1) {
                    val item = rooms.removeAt(pos)
                    rooms.add(pos + 1, item)
                    currentHouse = currentHouse.copy(rooms = rooms.toList())
                    store.updateHouse(currentHouse)
                    refreshList()
                }
            },
            onEdit = { pos, room ->
                showAddOrEditRoomDialog(context, store, currentHouse, editRoom = room, editIndex = pos) { updatedRoom ->
                    rooms[pos] = updatedRoom
                    currentHouse = currentHouse.copy(rooms = rooms.toList())
                    store.updateHouse(currentHouse)
                    refreshList()
                }
            },
            onDelete = { pos, room ->
                val entries = store.loadAll()
                val count = entries.count {
                    (it.houseName == currentHouse.name || it.houseId == currentHouse.id) &&
                    (it.roomName == room.name || it.location.contains(room.name))
                }
                val msg = if (count > 0) {
                    "当前空间已有 $count 件物品存放在【${room.name}】，删除该房间后物品仍会保留在空间中，确定要删除吗？"
                } else {
                    "确定要删除房间【${room.name}】吗？"
                }

                MaterialAlertDialogBuilder(context)
                    .setTitle("删除房间")
                    .setMessage(msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton("删除") { _, _ ->
                        rooms.removeAt(pos)
                        currentHouse = currentHouse.copy(rooms = rooms.toList())
                        store.updateHouse(currentHouse)
                        refreshList()
                        Toast.makeText(context, "已删除房间: ${room.name}", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        )

        binding.roomRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.roomRecyclerView.adapter = adapter
        adapter.submit(rooms.toList())

        binding.btnAddRoom.applyPressScaleAnimation(0.94f)
        binding.btnResetRooms.applyPressScaleAnimation(0.94f)

        // 新增房间
        binding.btnAddRoom.setOnClickListener {
            showAddOrEditRoomDialog(context, store, currentHouse, editRoom = null, editIndex = null) { newRoom ->
                rooms.add(newRoom)
                currentHouse = currentHouse.copy(rooms = rooms.toList())
                store.updateHouse(currentHouse)
                refreshList()
                Toast.makeText(context, "已成功添加房间: ${newRoom.name}", Toast.LENGTH_SHORT).show()
            }
        }

        // 恢复推荐
        binding.btnResetRooms.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle("恢复默认推荐房间")
                .setMessage("确定要将【${currentHouse.name}】恢复为默认推荐的 6 大标准房间（玄关、客厅、厨房、主卧、次卧、储物间）吗？")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val defaultRooms = HouseSpace.defaultRooms()
                    currentHouse = currentHouse.copy(rooms = defaultRooms)
                    store.updateHouse(currentHouse)
                    refreshList()
                    Toast.makeText(context, "已恢复推荐房间布局", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("【${currentHouse.name}】房间管理与自定义")
            .setView(binding.root)
            .setPositiveButton("完成", null)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        dialog.show()
    }

    /** 新增或编辑单个房间属性 */
    fun showAddOrEditRoomDialog(
        context: Context,
        store: DataStore,
        house: HouseSpace,
        editRoom: HouseRoom? = null,
        editIndex: Int? = null,
        onSaved: (HouseRoom) -> Unit
    ) {
        val binding = DialogInputRoomBinding.inflate(LayoutInflater.from(context))
        val isEdit = editRoom != null

        var selectedIcon = editRoom?.icon ?: "🛋️"
        var selectedColor = editRoom?.colorHex ?: "#10B981"

        if (isEdit) {
            binding.tvRoomDialogDesc.text = "正在修改【${editRoom!!.name}】的名称与样式"
            binding.inputRoomName.setText(editRoom.name)
        }

        // 1. 动态生成图标选择器
        val iconViews = mutableListOf<TextView>()
        binding.roomIconContainer.removeAllViews()

        for (icon in PRESET_ICONS) {
            val tv = TextView(context).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                val pad = dpToPx(context, 8)
                setPadding(pad, pad, pad, pad)
                val isSel = (icon == selectedIcon)
                setBackgroundResource(if (isSel) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
                applyPressScaleAnimation(0.90f)

                val lp = LinearLayout.LayoutParams(dpToPx(context, 44), dpToPx(context, 44)).apply {
                    marginEnd = dpToPx(context, 8)
                }
                layoutParams = lp

                setOnClickListener {
                    selectedIcon = icon
                    iconViews.forEach { v ->
                        val active = (v.text == selectedIcon)
                        v.setBackgroundResource(if (active) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
                    }
                }
            }
            iconViews.add(tv)
            binding.roomIconContainer.addView(tv)
        }

        // 2. 动态生成颜色选择器
        val colorViews = mutableListOf<TextView>()
        binding.roomColorContainer.removeAllViews()

        for (colorHex in PRESET_COLORS) {
            val tv = TextView(context).apply {
                text = if (colorHex == selectedColor) "✓" else ""
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                val c = Color.parseColor(colorHex)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    if (colorHex == selectedColor) {
                        setStroke(dpToPx(context, 2), Color.WHITE)
                    }
                }
                applyPressScaleAnimation(0.90f)

                val lp = LinearLayout.LayoutParams(dpToPx(context, 38), dpToPx(context, 38)).apply {
                    marginEnd = dpToPx(context, 8)
                }
                layoutParams = lp

                setOnClickListener {
                    selectedColor = colorHex
                    colorViews.forEachIndexed { idx, v ->
                        val hex = PRESET_COLORS[idx]
                        val isCurr = (hex == selectedColor)
                        v.text = if (isCurr) "✓" else ""
                        v.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor(hex))
                            if (isCurr) setStroke(dpToPx(context, 2), Color.WHITE)
                        }
                    }
                }
            }
            colorViews.add(tv)
            binding.roomColorContainer.addView(tv)
        }

        val dialogTitle = if (isEdit) "编辑房间" else "新增房间"

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(dialogTitle)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = binding.inputRoomName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(context, "请输入房间名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 计算房间相对几何坐标
                val (x, y, w, h) = if (isEdit && editRoom != null) {
                    listOf(editRoom.xPct, editRoom.yPct, editRoom.widthPct, editRoom.heightPct)
                } else {
                    computeAutoCoordinates(house.rooms.size)
                }

                val resultRoom = HouseRoom(
                    id = editRoom?.id ?: UUID.randomUUID().toString(),
                    name = name,
                    icon = selectedIcon,
                    colorHex = selectedColor,
                    xPct = x,
                    yPct = y,
                    widthPct = w,
                    heightPct = h
                )

                onSaved(resultRoom)
            }
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        dialog.show()
    }

    /** 自动为新增房间分配舒适平铺的网格坐标 */
    private fun computeAutoCoordinates(existingCount: Int): List<Float> {
        val row = (existingCount % 3)
        val col = if (existingCount < 3) 0 else 1
        val xPct = (0.05f + col * 0.46f).coerceIn(0.05f, 0.55f)
        val yPct = (0.05f + row * 0.31f).coerceIn(0.05f, 0.70f)
        val widthPct = 0.42f
        val heightPct = 0.28f
        return listOf(xPct, yPct, widthPct, heightPct)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    private val Int.sp: Float
        get() = this.toFloat()
}
