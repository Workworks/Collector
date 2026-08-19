package com.kfaino.diapertracker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject

/** 基于 SharedPreferences 的高可用持久化层，管理物品折旧、在役/退役待办归置、周期订阅资产与空间位置 */
class DataStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("collector_data", Context.MODE_PRIVATE)
    private val keyEntries = "entries_v4"
    private val keyHouses = "houses_v1"
    private val keyCategories = "custom_categories_v2"
    private val keyTheme = "app_theme_mode"

    companion object {
        // 通用默认分类
        val DEFAULT_CATEGORIES = listOf("数码", "日用品", "零食", "耗材", "贵重证件", "网络订阅")

        // 常用快捷数量单位
        val COMMON_UNITS = listOf("件", "台", "个", "套", "张", "片", "包", "箱", "瓶", "盒", "本")

        // 常见待办归置渠道
        val RETIRED_ACTIONS = listOf(
            "📦 挂闲鱼代售",
            "📱 挂转转二手",
            "🎁 赠送亲友",
            "♻️ 环保回收 / 以旧换新",
            "🗄️ 封箱入库收藏",
            "🗑️ 报废丢弃"
        )

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        fun applyThemeMode(mode: Int) {
            when (mode) {
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    // ==================== 物品出入库、折旧与订阅记录 ====================

    fun loadAll(): List<Entry> {
        val raw = prefs.getString(keyEntries, null) ?: prefs.getString("entries_v3", null) ?: prefs.getString("entries_v2", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val histArr = o.optJSONArray("loc_hist")
                val histList = mutableListOf<LocationMovement>()
                if (histArr != null) {
                    for (h in 0 until histArr.length()) {
                        val ho = histArr.getJSONObject(h)
                        histList.add(
                            LocationMovement(
                                location = ho.optString("loc", ""),
                                houseName = ho.optString("h_name", "我的家"),
                                roomName = ho.optString("r_name", ""),
                                pinX = ho.optDouble("px", -1.0).toFloat(),
                                pinY = ho.optDouble("py", -1.0).toFloat(),
                                movedAt = ho.optLong("ts", System.currentTimeMillis()),
                                note = ho.optString("note", "")
                            )
                        )
                    }
                }

                val ts = o.optLong("ts", System.currentTimeMillis())
                val pDate = o.optLong("p_date", ts)

                result.add(
                    Entry(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        category = o.optString("cat", "数码"),
                        brand = o.optString("brand", "物品"),
                        qty = o.optInt("qty", 1),
                        price = o.optDouble("price", 0.0),
                        currentValuation = o.optDouble("cur_val", 0.0),
                        purchaseDate = pDate,
                        ts = ts,
                        isIn = o.optBoolean("in", true),
                        notes = o.optString("notes", ""),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", ""),
                        houseId = o.optString("h_id", "default_house"),
                        houseName = o.optString("h_name", "我的家"),
                        roomName = o.optString("r_name", ""),
                        pinX = o.optDouble("px", -1.0).toFloat(),
                        pinY = o.optDouble("py", -1.0).toFloat(),
                        locationHistory = histList,
                        isImportant = o.optBoolean("imp", false),
                        reminderEnabled = o.optBoolean("rem_en", false),
                        reminderIntervalDays = o.optInt("rem_int", 1),
                        reminderTime = o.optString("rem_tm", "09:00"),
                        lastCheckedAt = o.optLong("chk_ts", 0L),
                        isRetired = o.optBoolean("is_ret", false),
                        retiredAt = o.optLong("ret_at", 0L),
                        retiredAction = o.optString("ret_act", ""),
                        retiredSoldPrice = o.optDouble("ret_sp", 0.0),
                        retiredNote = o.optString("ret_note", ""),
                        isSubscription = o.optBoolean("is_sub", false),
                        subCycle = o.optString("sub_cyc", "按月"),
                        subNextBillingDate = o.optLong("sub_nxt", 0L),
                        subAutoRenew = o.optBoolean("sub_rnw", true),
                        assetType = o.optString("a_type", if (o.optBoolean("is_sub", false)) "subscription" else "consumable"),
                        manufactureDate = o.optLong("m_date", 0L),
                        expiryDate = o.optLong("e_date", 0L)
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAll(entries: List<Entry>) {
        val arr = JSONArray()
        for (e in entries) {
            val histArr = JSONArray()
            for (h in e.locationHistory) {
                histArr.put(
                    JSONObject()
                        .put("loc", h.location)
                        .put("h_name", h.houseName)
                        .put("r_name", h.roomName)
                        .put("px", h.pinX.toDouble())
                        .put("py", h.pinY.toDouble())
                        .put("ts", h.movedAt)
                        .put("note", h.note)
                )
            }

            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("cat", e.category)
                    .put("brand", e.brand)
                    .put("qty", e.qty)
                    .put("price", e.price)
                    .put("cur_val", e.currentValuation)
                    .put("p_date", e.purchaseDate)
                    .put("ts", e.ts)
                    .put("in", e.isIn)
                    .put("notes", e.notes)
                    .put("unit", e.unit)
                    .put("loc", e.location)
                    .put("h_id", e.houseId)
                    .put("h_name", e.houseName)
                    .put("r_name", e.roomName)
                    .put("px", e.pinX.toDouble())
                    .put("py", e.pinY.toDouble())
                    .put("loc_hist", histArr)
                    .put("imp", e.isImportant)
                    .put("rem_en", e.reminderEnabled)
                    .put("rem_int", e.reminderIntervalDays)
                    .put("rem_tm", e.reminderTime)
                    .put("chk_ts", e.lastCheckedAt)
                    .put("is_ret", e.isRetired)
                    .put("ret_at", e.retiredAt)
                    .put("ret_act", e.retiredAction)
                    .put("ret_sp", e.retiredSoldPrice)
                    .put("ret_note", e.retiredNote)
                    .put("is_sub", e.isSubscription)
                    .put("sub_cyc", e.subCycle)
                    .put("sub_nxt", e.subNextBillingDate)
                    .put("sub_rnw", e.subAutoRenew)
                    .put("a_type", e.assetType)
                    .put("m_date", e.manufactureDate)
                    .put("e_date", e.expiryDate)
            )
        }
        prefs.edit().putString(keyEntries, arr.toString()).apply()
    }

    fun updateEntry(index: Int, newEntry: Entry): Boolean {
        val list = loadAll().toMutableList()
        if (index in 0 until list.size) {
            val oldEntry = list[index]
            // 如果位置发生了挪动，自动追加位置历史记录
            val finalEntry = if (oldEntry.location.isNotBlank() && oldEntry.location != newEntry.location) {
                val newHist = oldEntry.locationHistory.toMutableList()
                newHist.add(
                    0,
                    LocationMovement(
                        location = oldEntry.location,
                        houseName = oldEntry.houseName,
                        roomName = oldEntry.roomName,
                        pinX = oldEntry.pinX,
                        pinY = oldEntry.pinY,
                        movedAt = System.currentTimeMillis(),
                        note = "原位置变更为【${newEntry.location}】"
                    )
                )
                newEntry.copy(locationHistory = newHist)
            } else {
                newEntry
            }

            list[index] = finalEntry
            saveAll(list)
            return true
        }
        return false
    }

    fun deleteEntryAt(index: Int): Boolean {
        val list = loadAll().toMutableList()
        if (index in 0 until list.size) {
            list.removeAt(index)
            saveAll(list)
            return true
        }
        return false
    }

    fun setRetired(entryId: String, isRetired: Boolean, action: String = "挂闲鱼代售", soldPrice: Double = 0.0, note: String = "") {
        val list = loadAll().toMutableList()
        val idx = list.indexOfFirst { it.id == entryId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isRetired = isRetired,
                retiredAt = if (isRetired) System.currentTimeMillis() else 0L,
                retiredAction = if (isRetired) action else "",
                retiredSoldPrice = soldPrice,
                retiredNote = note
            )
            saveAll(list)
        }
    }

    fun clearAllData() {
        prefs.edit().remove(keyEntries).apply()
    }

    fun getLastUsedUnit(): String {
        return prefs.getString("last_used_unit", "件") ?: "件"
    }

    fun setLastUsedUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString("last_used_unit", trimmed).apply()
        }
    }

    // ==================== 重要物品与订阅提醒 ====================

    fun getImportantEntries(): List<Entry> {
        return loadAll().filter { it.isImportant || it.reminderEnabled }
    }

    fun getSubscriptionEntries(): List<Entry> {
        return loadAll().filter { it.isSubscription }
    }

    fun getNonSubscriptionEntries(): List<Entry> {
        return loadAll().filter { !it.isSubscription }
    }

    fun confirmItemChecked(entryId: String) {
        val list = loadAll().toMutableList()
        val idx = list.indexOfFirst { it.id == entryId }
        if (idx != -1) {
            list[idx] = list[idx].copy(lastCheckedAt = System.currentTimeMillis())
            saveAll(list)
        }
    }

    // ==================== 多空间/家庭空间管理 ====================

    fun getHouses(): List<HouseSpace> {
        val raw = prefs.getString(keyHouses, null)
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
                                id = ro.optString("id", java.util.UUID.randomUUID().toString()),
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
        } catch (_: Exception) {
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
        prefs.edit().putString(keyHouses, arr.toString()).apply()
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

    // ==================== 通用分类分组管理 ====================

    fun getCategories(): List<String> {
        val raw = prefs.getString(keyCategories, null)
        val list = mutableListOf<String>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)?.trim()
                    if (!s.isNullOrEmpty() && !list.contains(s)) {
                        list.add(s)
                    }
                }
            } catch (_: Exception) {}
        }

        if (list.isEmpty()) {
            list.addAll(DEFAULT_CATEGORIES)
        }

        val existingEntries = loadAll()
        for (entry in existingEntries) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !list.contains(cat)) {
                list.add(cat)
            }
        }

        return list
    }

    fun saveCategories(categories: List<String>) {
        val arr = JSONArray()
        for (c in categories) {
            val trimmed = c.trim()
            if (trimmed.isNotEmpty()) {
                arr.put(trimmed)
            }
        }
        prefs.edit().putString(keyCategories, arr.toString()).apply()
    }

    fun addCategory(category: String): Boolean {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return false
        val current = getCategories().toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        current.add(trimmed)
        saveCategories(current)
        return true
    }

    fun deleteCategory(category: String): Boolean {
        val current = getCategories().toMutableList()
        val removed = current.remove(category)
        if (removed) {
            saveCategories(current)
        }
        return removed
    }

    fun resetCategories(): List<String> {
        val defaults = DEFAULT_CATEGORIES.toMutableList()
        for (entry in loadAll()) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !defaults.contains(cat)) {
                defaults.add(cat)
            }
        }
        saveCategories(defaults)
        return defaults
    }

    fun isPresetCategory(category: String): Boolean {
        return DEFAULT_CATEGORIES.contains(category)
    }

    // ==================== 主题设置（深色/浅色/系统） ====================

    fun getThemeMode(): Int {
        return prefs.getInt(keyTheme, THEME_SYSTEM)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(keyTheme, mode).apply()
        applyThemeMode(mode)
    }

    // ==================== GitHub 更新仓库设置 ====================

    fun getGithubRepo(): String {
        return prefs.getString("github_repo", "Workworks/Collector") ?: "Workworks/Collector"
    }

    fun setGithubRepo(repo: String) {
        prefs.edit().putString("github_repo", repo.trim()).apply()
    }

    // ==================== 通知提醒设置 ====================

    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean("reminders_enabled", true)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
    }

    fun getNotificationHour(): Int {
        return prefs.getInt("reminder_hour", 9)
    }

    fun getNotificationMinute(): Int {
        return prefs.getInt("reminder_minute", 0)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
    }

    // ==================== 备份与恢复 ====================

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        val catArr = JSONArray()
        for (c in getCategories()) catArr.put(c)
        root.put("categories", catArr)

        val entryArr = JSONArray()
        for (e in loadAll()) {
            val histArr = JSONArray()
            for (h in e.locationHistory) {
                histArr.put(
                    JSONObject()
                        .put("loc", h.location)
                        .put("h_name", h.houseName)
                        .put("r_name", h.roomName)
                        .put("ts", h.movedAt)
                        .put("note", h.note)
                )
            }

            entryArr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("cat", e.category)
                    .put("brand", e.brand)
                    .put("qty", e.qty)
                    .put("price", e.price)
                    .put("cur_val", e.currentValuation)
                    .put("p_date", e.purchaseDate)
                    .put("ts", e.ts)
                    .put("in", e.isIn)
                    .put("notes", e.notes)
                    .put("unit", e.unit)
                    .put("loc", e.location)
                    .put("h_id", e.houseId)
                    .put("h_name", e.houseName)
                    .put("r_name", e.roomName)
                    .put("px", e.pinX.toDouble())
                    .put("py", e.pinY.toDouble())
                    .put("loc_hist", histArr)
                    .put("imp", e.isImportant)
                    .put("rem_en", e.reminderEnabled)
                    .put("rem_int", e.reminderIntervalDays)
                    .put("chk_ts", e.lastCheckedAt)
                    .put("is_ret", e.isRetired)
                    .put("ret_at", e.retiredAt)
                    .put("ret_act", e.retiredAction)
                    .put("ret_sp", e.retiredSoldPrice)
                    .put("ret_note", e.retiredNote)
                    .put("is_sub", e.isSubscription)
                    .put("sub_cyc", e.subCycle)
                    .put("sub_nxt", e.subNextBillingDate)
                    .put("sub_rnw", e.subAutoRenew)
                    .put("a_type", e.assetType)
                    .put("m_date", e.manufactureDate)
                    .put("e_date", e.expiryDate)
            )
        }
        root.put("entries", entryArr)
        return root.toString(2)
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val catArr = root.optJSONArray("categories")
            if (catArr != null) {
                val cats = getCategories().toMutableList()
                for (i in 0 until catArr.length()) {
                    val c = catArr.optString(i).trim()
                    if (c.isNotEmpty() && !cats.contains(c)) {
                        cats.add(c)
                    }
                }
                if (cats.isNotEmpty()) {
                    saveCategories(cats)
                }
            }

            val entryArr = root.optJSONArray("entries")
            if (entryArr != null) {
                val list = mutableListOf<Entry>()
                for (i in 0 until entryArr.length()) {
                    val o = entryArr.getJSONObject(i)
                    val histArr = o.optJSONArray("loc_hist")
                    val histList = mutableListOf<LocationMovement>()
                    if (histArr != null) {
                        for (h in 0 until histArr.length()) {
                            val ho = histArr.getJSONObject(h)
                            histList.add(
                                LocationMovement(
                                    location = ho.optString("loc", ""),
                                    houseName = ho.optString("h_name", "我的家"),
                                    roomName = ho.optString("r_name", ""),
                                    movedAt = ho.optLong("ts", System.currentTimeMillis()),
                                    note = ho.optString("note", "")
                                )
                            )
                        }
                    }

                    val ts = o.optLong("ts", System.currentTimeMillis())
                    val pDate = o.optLong("p_date", ts)

                    list.add(
                        Entry(
                            id = o.optString("id", java.util.UUID.randomUUID().toString()),
                            category = o.optString("cat", "数码"),
                            brand = o.optString("brand", "物品"),
                            qty = o.optInt("qty", 1),
                            price = o.optDouble("price", 0.0),
                            currentValuation = o.optDouble("cur_val", 0.0),
                            purchaseDate = pDate,
                            ts = ts,
                            isIn = o.optBoolean("in", true),
                            notes = o.optString("notes", ""),
                            unit = o.optString("unit", "件"),
                            location = o.optString("loc", ""),
                            houseId = o.optString("h_id", "default_house"),
                            houseName = o.optString("h_name", "我的家"),
                            roomName = o.optString("r_name", ""),
                            pinX = o.optDouble("px", -1.0).toFloat(),
                            pinY = o.optDouble("py", -1.0).toFloat(),
                            locationHistory = histList,
                            isImportant = o.optBoolean("imp", false),
                            reminderEnabled = o.optBoolean("rem_en", false),
                            reminderIntervalDays = o.optInt("rem_int", 1),
                            reminderTime = o.optString("rem_tm", "09:00"),
                            lastCheckedAt = o.optLong("chk_ts", 0L),
                            isRetired = o.optBoolean("is_ret", false),
                            retiredAt = o.optLong("ret_at", 0L),
                            retiredAction = o.optString("ret_act", ""),
                            retiredSoldPrice = o.optDouble("ret_sp", 0.0),
                            retiredNote = o.optString("ret_note", ""),
                            isSubscription = o.optBoolean("is_sub", false),
                            subCycle = o.optString("sub_cyc", "按月"),
                            subNextBillingDate = o.optLong("sub_nxt", 0L),
                            subAutoRenew = o.optBoolean("sub_rnw", true),
                            assetType = o.optString("a_type", if (o.optBoolean("is_sub", false)) "subscription" else "consumable"),
                            manufactureDate = o.optLong("m_date", 0L),
                            expiryDate = o.optLong("e_date", 0L)
                        )
                    )
                }
                saveAll(list)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ==================== 触感震动反馈配置 ====================

    fun isHapticFeedbackEnabled(): Boolean {
        return prefs.getBoolean("haptic_feedback_enabled", true)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
    }
}
