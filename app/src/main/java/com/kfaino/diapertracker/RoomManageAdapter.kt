package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemManageRoomBinding

class RoomManageAdapter(
    private val dataStore: DataStore,
    private val houseSpace: HouseSpace,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onEdit: (Int, HouseRoom) -> Unit,
    private val onDelete: (Int, HouseRoom) -> Unit
) : RecyclerView.Adapter<RoomManageAdapter.VH>() {

    private var items: List<HouseRoom> = emptyList()

    fun submit(list: List<HouseRoom>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemManageRoomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val room = items[position]
        val b = holder.binding

        b.roomIcon.text = room.icon
        try {
            val color = Color.parseColor(room.colorHex)
            b.roomIcon.backgroundTintList = ColorStateList.valueOf(color)
        } catch (_: Exception) {
            b.roomIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        }

        b.roomName.text = room.name

        val entries = dataStore.loadAll()
        val inRoomCount = entries.count {
            (it.houseName == houseSpace.name || it.houseId == houseSpace.id) &&
            (it.roomName == room.name || it.location.contains(room.name))
        }
        b.roomItemCount.text = if (inRoomCount > 0) "存放 $inRoomCount 件物品" else "暂无存放物品"

        b.btnMoveUp.isEnabled = position > 0
        b.btnMoveUp.alpha = if (position > 0) 1.0f else 0.3f
        b.btnMoveUp.setOnClickListener { onMoveUp(position) }

        b.btnMoveDown.isEnabled = position < items.size - 1
        b.btnMoveDown.alpha = if (position < items.size - 1) 1.0f else 0.3f
        b.btnMoveDown.setOnClickListener { onMoveDown(position) }

        b.btnEditRoom.setOnClickListener { onEdit(position, room) }
        b.btnDeleteRoom.setOnClickListener { onDelete(position, room) }
    }

    class VH(val binding: ItemManageRoomBinding) : RecyclerView.ViewHolder(binding.root)
}
