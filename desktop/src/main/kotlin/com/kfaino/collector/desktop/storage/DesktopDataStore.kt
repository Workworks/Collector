package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 跨平台桌面端高可靠本地持久化存储
 * 存储路径: ~/.collector/collector_data.json (Linux & macOS 统一规范)
 * 桌面端为独立轻量端（功能子集，支持基础物品管理与轻量配置）
 */
class DesktopDataStore {

    private val dataDir: File = run {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".collector")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val dataFile = File(dataDir, "collector_data.json")
    private val configFile = File(dataDir, "config.json")

    private var inMemoryEntries = mutableListOf<Entry>()
    private var inMemoryCategories = mutableListOf<String>()
    private var inMemoryHouses = mutableListOf<House>()

    private var simpleMode = false
    private var webDavUrl = "https://dav.jianguoyun.com/dav/"
    private var webDavUsername = ""
    private var webDavPassword = ""

    companion object {
        val DEFAULT_CATEGORIES = listOf("数码", "日用品", "零食", "耗材", "贵重证件", "网络订阅")
        val COMMON_UNITS = listOf("件", "台", "个", "套", "张", "片", "包", "箱", "瓶", "盒", "本")
        val DEFAULT_ROOMS = listOf("玄关", "客厅", "主卧", "次卧", "厨房", "卫生间", "储物间", "阳台")
    }

    init {
        loadAll()
        loadConfig()
    }

    // ==================== 资产与出入库记录 ====================

    @Synchronized
    fun loadAll(): List<Entry> {
        if (!dataFile.exists()) {
            inMemoryEntries = mutableListOf()
            inMemoryCategories = DEFAULT_CATEGORIES.toMutableList()
            inMemoryHouses = mutableListOf(House(name = "我的家", rooms = DEFAULT_ROOMS.map { Room(it) }))
            saveAll()
            return inMemoryEntries
        }

        return try {
            val content = dataFile.readText(StandardCharsets.UTF_8)
            val root = JSONObject(content)
            val arr = root.optJSONArray("entries") ?: JSONArray()
            val list = mutableListOf<Entry>()

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

                val cycleStr = o.optString("sub_cycle", "MONTHLY")
                val cycle = try { SubCycle.valueOf(cycleStr) } catch (e: Exception) { SubCycle.MONTHLY }

                list.add(
                    Entry(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        brand = o.optString("brand", ""),
                        category = o.optString("category", "日用品"),
                        price = o.optDouble("price", 0.0),
                        qty = o.optInt("qty", 1),
                        unit = o.optString("unit", "件"),
                        location = o.optString("location", ""),
                        houseName = o.optString("houseName", "我的家"),
                        roomName = o.optString("roomName", ""),
                        pinX = o.optDouble("pinX", -1.0).toFloat(),
                        pinY = o.optDouble("pinY", -1.0).toFloat(),
                        locationHistory = histList,
                        isIn = o.optBoolean("isIn", true),
                        ts = o.optLong("ts", System.currentTimeMillis()),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("photoPath", ""),
                        receiptPath = o.optString("receiptPath", ""),
                        barcode = o.optString("barcode", ""),
                        isDepreciating = o.optBoolean("is_depreciating", true),
                        mfgDate = o.optLong("mfg_date", 0L),
                        expDate = o.optLong("exp_date", 0L),
                        isDurable = o.optBoolean("is_durable", false),
                        durableStartDate = o.optLong("durable_start_date", 0L),
                        isConsumable = o.optBoolean("is_consumable", false),
                        originalPrice = o.optDouble("original_price", o.optDouble("price", 0.0)),
                        purchaseDate = o.optLong("purchase_date", o.optLong("ts", System.currentTimeMillis())),
                        currentValuation = o.optDouble("current_valuation", o.optDouble("price", 0.0)),
                        lastValuationDate = o.optLong("last_valuation_date", o.optLong("ts", System.currentTimeMillis())),
                        targetResidualRate = o.optDouble("target_residual_rate", 0.1),
                        expectedLifeYears = o.optDouble("expected_life_years", 3.0),
                        isRetired = o.optBoolean("is_retired", false),
                        retiredDate = o.optLong("retired_date", 0L),
                        retiredAction = o.optString("retired_action", ""),
                        retiredSoldPrice = o.optDouble("retired_sold_price", 0.0),
                        retiredNote = o.optString("retired_note", ""),
                        isSubscription = o.optBoolean("is_sub", false),
                        subPrice = o.optDouble("sub_price", o.optDouble("price", 0.0)),
                        subCycle = cycle,
                        subStartDate = o.optLong("sub_start_date", o.optLong("ts", System.currentTimeMillis())),
                        subNextBillingDate = o.optLong("sub_next_billing_date", o.optLong("ts", System.currentTimeMillis()) + 30L * 24 * 60 * 60 * 1000),
                        subAutoRenew = o.optBoolean("sub_auto_renew", true),
                        isImportant = o.optBoolean("is_important", false),
                        reminderEnabled = o.optBoolean("reminder_enabled", false),
                        reminderIntervalDays = o.optInt("reminder_interval_days", 1),
                        lastCheckedDate = o.optLong("last_checked_date", o.optLong("ts", System.currentTimeMillis()))
                    )
                )
            }

            // 分类加载
            val catArr = root.optJSONArray("categories")
            inMemoryCategories = if (catArr != null && catArr.length() > 0) {
                val cl = mutableListOf<String>()
                for (c in 0 until catArr.length()) cl.add(catArr.getString(c))
                cl
            } else {
                DEFAULT_CATEGORIES.toMutableList()
            }

            inMemoryEntries = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            inMemoryEntries
        }
    }

    @Synchronized
    fun saveAll(list: List<Entry> = inMemoryEntries) {
        inMemoryEntries = list.toMutableList()
        val root = JSONObject()
        val arr = JSONArray()

        for (e in inMemoryEntries) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("brand", e.brand)
            o.put("category", e.category)
            o.put("price", e.price)
            o.put("qty", e.qty)
            o.put("unit", e.unit)
            o.put("location", e.location)
            o.put("houseName", e.houseName)
            o.put("roomName", e.roomName)
            o.put("pinX", e.pinX)
            o.put("pinY", e.pinY)
            o.put("isIn", e.isIn)
            o.put("ts", e.ts)
            o.put("notes", e.notes)
            o.put("photoPath", e.photoPath)
            o.put("receiptPath", e.receiptPath)
            o.put("barcode", e.barcode)
            o.put("is_depreciating", e.isDepreciating)
            o.put("mfg_date", e.mfgDate)
            o.put("exp_date", e.expDate)
            o.put("is_durable", e.isDurable)
            o.put("durable_start_date", e.durableStartDate)
            o.put("is_consumable", e.isConsumable)
            o.put("original_price", e.originalPrice)
            o.put("purchase_date", e.purchaseDate)
            o.put("current_valuation", e.currentValuation)
            o.put("last_valuation_date", e.lastValuationDate)
            o.put("target_residual_rate", e.targetResidualRate)
            o.put("expected_life_years", e.expectedLifeYears)
            o.put("is_retired", e.isRetired)
            o.put("retired_date", e.retiredDate)
            o.put("retired_action", e.retiredAction)
            o.put("retired_sold_price", e.retiredSoldPrice)
            o.put("retired_note", e.retiredNote)
            o.put("is_sub", e.isSubscription)
            o.put("sub_price", e.subPrice)
            o.put("sub_cycle", e.subCycle.name)
            o.put("sub_start_date", e.subStartDate)
            o.put("sub_next_billing_date", e.subNextBillingDate)
            o.put("sub_auto_renew", e.subAutoRenew)
            o.put("is_important", e.isImportant)
            o.put("reminder_enabled", e.reminderEnabled)
            o.put("reminder_interval_days", e.reminderIntervalDays)
            o.put("last_checked_date", e.lastCheckedDate)

            if (e.locationHistory.isNotEmpty()) {
                val histArr = JSONArray()
                for (h in e.locationHistory) {
                    val ho = JSONObject()
                    ho.put("loc", h.location)
                    ho.put("h_name", h.houseName)
                    ho.put("r_name", h.roomName)
                    ho.put("px", h.pinX)
                    ho.put("py", h.pinY)
                    ho.put("ts", h.movedAt)
                    ho.put("note", h.note)
                    histArr.put(ho)
                }
                o.put("loc_hist", histArr)
            }
            arr.put(o)
        }

        root.put("entries", arr)
        root.put("categories", JSONArray(inMemoryCategories))
        root.put("updatedAt", System.currentTimeMillis())

        dataFile.writeText(root.toString(2), StandardCharsets.UTF_8)
    }

    fun addEntry(entry: Entry) {
        val list = inMemoryEntries.toMutableList()
        list.add(0, entry)
        saveAll(list)
    }

    fun updateEntry(entry: Entry) {
        val list = inMemoryEntries.toMutableList()
        val idx = list.indexOfFirst { it.id == entry.id }
        if (idx != -1) {
            list[idx] = entry
            saveAll(list)
        }
    }

    fun deleteEntry(id: String) {
        val list = inMemoryEntries.toMutableList()
        list.removeAll { it.id == id }
        saveAll(list)
    }

    fun getCategories(): List<String> = inMemoryCategories

    fun setCategories(cats: List<String>) {
        inMemoryCategories = cats.toMutableList()
        saveAll()
    }

    // ==================== 简易模式与 WebDAV 设置 ====================

    fun isSimpleMode(): Boolean = simpleMode
    fun setSimpleMode(enabled: Boolean) {
        simpleMode = enabled
        saveConfig()
    }

    fun getWebDavUrl(): String = webDavUrl
    fun setWebDavUrl(url: String) {
        webDavUrl = url.trim()
        saveConfig()
    }

    fun getWebDavUsername(): String = webDavUsername
    fun setWebDavUsername(u: String) {
        webDavUsername = u.trim()
        saveConfig()
    }

    fun getWebDavPassword(): String = webDavPassword
    fun setWebDavPassword(p: String) {
        webDavPassword = p
        saveConfig()
    }

    private fun loadConfig() {
        if (!configFile.exists()) return
        try {
            val json = JSONObject(configFile.readText(StandardCharsets.UTF_8))
            simpleMode = json.optBoolean("simple_mode", false)
            webDavUrl = json.optString("webdav_url", "https://dav.jianguoyun.com/dav/")
            webDavUsername = json.optString("webdav_user", "")
            webDavPassword = json.optString("webdav_pass", "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveConfig() {
        try {
            val json = JSONObject()
            json.put("simple_mode", simpleMode)
            json.put("webdav_url", webDavUrl)
            json.put("webdav_user", webDavUsername)
            json.put("webdav_pass", webDavPassword)
            configFile.writeText(json.toString(2), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== JSON & CSV 导入导出 ====================

    fun exportJson(): String {
        return if (dataFile.exists()) dataFile.readText(StandardCharsets.UTF_8) else "{}"
    }

    fun importJson(jsonStr: String): Boolean {
        return try {
            dataFile.writeText(jsonStr, StandardCharsets.UTF_8)
            loadAll()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM
        sb.append("物品ID,品牌/名称,分类,方向,单价,数量,单位,总额,放置位置,记录时间,状态,日均消费(元/天),备注\n")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

        for (e in inMemoryEntries) {
            val dir = if (e.isIn) "入库/拥有" else "出库/消耗"
            val total = e.price * e.qty
            val time = sdf.format(java.util.Date(e.ts))
            val status = if (e.isRetired) "已退役(${e.retiredAction})" else "在役中"
            val daily = String.format(java.util.Locale.getDefault(), "%.2f", e.getDailyCost())

            fun csvCell(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            sb.append("${csvCell(e.id)},${csvCell(e.brand)},${csvCell(e.category)},$dir,${e.price},${e.qty},${csvCell(e.unit)},$total,${csvCell(e.location)},$time,$status,$daily,${csvCell(e.notes)}\n")
        }
        return sb.toString()
    }
}
