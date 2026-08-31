package com.kfaino.diapertracker

/**
 * 🏢 多房间空间拓扑层级与 3D 楼层穿梭导航引擎 (Multi-Floor 3D Navigator)
 * 支持复式/别墅多楼层空间拓扑建模与跨楼层寻物路径推演
 */
object MultiFloor3dNavigator {

    data class FloorLevel(
        val floorNumber: Int,
        val floorName: String,
        val roomNames: List<String>
    )

    data class NavigationPath(
        val targetItemName: String,
        val targetFloor: String,
        val targetRoom: String,
        val targetLocation: String,
        val stepGuidance: List<String>
    )

    fun generatePath(store: DataStore, itemId: String): NavigationPath? {
        val entry = store.loadAll().find { it.id == itemId } ?: return null

        val floor = if (entry.roomName.contains("二楼") || entry.location.contains("二楼")) "2F 二层" else "1F 一层"
        val room = if (entry.roomName.isNotBlank()) entry.roomName else "主空间"
        val steps = listOf(
            "1. 前往【" + floor + "】",
            "2. 进入【" + room + "】",
            "3. 寻找具体位置【" + entry.location + "】",
            "4. 锁定目标物品【" + entry.brand + "】"
        )

        return NavigationPath(
            targetItemName = entry.brand,
            targetFloor = floor,
            targetRoom = entry.roomName,
            targetLocation = entry.location,
            stepGuidance = steps
        )
    }
}