package com.kfaino.diapertracker

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 🏠 空间与多房屋房间持久化仓储 (Space Repository)
 * 封装多套家庭空间房屋 (HouseSpace) 与房间 (HouseRoom) 增删改查、平面图坐标与默认空间切换。
 * 作为 DataStore 门面下沉的专用仓储，严格保持 SharedPreferences Key 完全不变。
 */
class SpaceRepository(private val prefs: SharedPreferences) {

    companion object {
        private const val TAG = "SpaceRepository"
        const val KEY_HOUSES = "houses_v1"
    }

    fun getHouses(): List<HouseSpace> {
        val raw = prefs.getString(KEY_HOUSES, null)
        if (raw == null) {
            val defaultList = listOf(
                HouseSpace(id = "house_default", name = "🏠 自己的家", type = "住宅", isDefault = true),
                HouseSpace(id = "house_parents", name = "🏡 父母家", type = "住宅", isDefault = false),
                HouseSpace(id = "house_office", name = "🏢 办公室", type = "办公", isDefault = false)
            )
            saveHouses(defaultList)
            return defaultList
        }

        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<HouseSpace>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val roomArr = o.optJSONArray("rooms")
                val rooms = mutableListOf<HouseRoom>()
                if (roomArr != null) {
                    for (r in 0 until roomArr.length()) {
                        val ro = roomArr.getJSONObject(r)
                        rooms.add(
                            HouseRoom(
                                id = ro.optString("id", UUID.randomUUID().toString()),
                                name = ro.optString("name", "房间"),
                                icon = ro.optString("icon", "🚪"),
                                colorHex = ro.optString("color", "#10B981"),
                                xPct = ro.optDouble("x", 0.1).toFloat(),
                                yPct = ro.optDouble("y", 0.1).toFloat(),
                                widthPct = ro.optDouble("w", 0.35).toFloat(),
                                heightPct = ro.optDouble("h", 0.35).toFloat()
                            )
                        )
                    }
                }
                result.add(
                    HouseSpace(
                        id = o.optString("id", "house_$i"),
                        name = o.optString("name", "我的家"),
                        type = o.optString("type", "住宅"),
                        rooms = if (rooms.isNotEmpty()) rooms else HouseSpace.defaultRooms(),
                        isDefault = o.optBoolean("is_def", i == 0)
                    )
                )
            }
            if (result.isEmpty()) HouseSpace.defaultRooms()
            result
        } catch (e: Exception) {
            Log.w(TAG, "getHouses 解析失败: $raw", e)
            listOf(HouseSpace(id = "house_default", name = "🏠 自己的家", type = "住宅", isDefault = true))
        }
    }

    fun saveHouses(houses: List<HouseSpace>) {
        val arr = JSONArray()
        for (h in houses) {
            val roomArr = JSONArray()
            for (r in h.rooms) {
                roomArr.put(
                    JSONObject()
                        .put("id", r.id)
                        .put("name", r.name)
                        .put("icon", r.icon)
                        .put("color", r.colorHex)
                        .put("x", r.xPct.toDouble())
                        .put("y", r.yPct.toDouble())
                        .put("w", r.widthPct.toDouble())
                        .put("h", r.heightPct.toDouble())
                )
            }
            arr.put(
                JSONObject()
                    .put("id", h.id)
                    .put("name", h.name)
                    .put("type", h.type)
                    .put("rooms", roomArr)
                    .put("is_def", h.isDefault)
            )
        }
        JsonCollectionWriter.save(prefs, KEY_HOUSES, arr)
    }

    fun addHouse(name: String, type: String = "住宅"): HouseSpace {
        val houses = getHouses().toMutableList()
        val newHouse = HouseSpace(
            id = "house_" + System.currentTimeMillis(),
            name = name,
            type = type,
            rooms = HouseSpace.defaultRooms(),
            isDefault = houses.isEmpty()
        )
        houses.add(newHouse)
        saveHouses(houses)
        return newHouse
    }

    fun deleteHouse(houseId: String): Boolean {
        val houses = getHouses().toMutableList()
        val removed = houses.removeAll { it.id == houseId }
        if (removed && houses.isNotEmpty()) {
            if (houses.none { it.isDefault }) {
                houses[0] = houses[0].copy(isDefault = true)
            }
            saveHouses(houses)
        }
        return removed
    }

    fun updateHouse(updatedHouse: HouseSpace): Boolean {
        val houses = getHouses().toMutableList()
        val idx = houses.indexOfFirst { it.id == updatedHouse.id }
        if (idx != -1) {
            houses[idx] = updatedHouse
            saveHouses(houses)
            return true
        }
        return false
    }

    fun addRoomToHouse(houseId: String, room: HouseRoom): Boolean {
        val houses = getHouses().toMutableList()
        val idx = houses.indexOfFirst { it.id == houseId }
        if (idx != -1) {
            val house = houses[idx]
            val currentRooms = house.rooms.toMutableList()
            currentRooms.add(room)
            houses[idx] = house.copy(rooms = currentRooms)
            saveHouses(houses)
            return true
        }
        return false
    }

    fun updateRoomInHouse(houseId: String, room: HouseRoom): Boolean {
        val houses = getHouses().toMutableList()
        val idx = houses.indexOfFirst { it.id == houseId }
        if (idx != -1) {
            val house = houses[idx]
            val currentRooms = house.rooms.toMutableList()
            val rIdx = currentRooms.indexOfFirst { it.id == room.id }
            if (rIdx != -1) {
                currentRooms[rIdx] = room
                houses[idx] = house.copy(rooms = currentRooms)
                saveHouses(houses)
                return true
            }
        }
        return false
    }

    fun deleteRoomFromHouse(houseId: String, roomId: String): Boolean {
        val houses = getHouses().toMutableList()
        val idx = houses.indexOfFirst { it.id == houseId }
        if (idx != -1) {
            val house = houses[idx]
            val currentRooms = house.rooms.toMutableList()
            val removed = currentRooms.removeAll { it.id == roomId }
            if (removed) {
                houses[idx] = house.copy(rooms = currentRooms)
                saveHouses(houses)
                return true
            }
        }
        return false
    }

    fun resetRoomsInHouse(houseId: String): List<HouseRoom> {
        val houses = getHouses().toMutableList()
        val idx = houses.indexOfFirst { it.id == houseId }
        if (idx != -1) {
            val defaults = HouseSpace.defaultRooms()
            houses[idx] = houses[idx].copy(rooms = defaults)
            saveHouses(houses)
            return defaults
        }
        return HouseSpace.defaultRooms()
    }
}
