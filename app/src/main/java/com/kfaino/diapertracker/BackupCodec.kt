package com.kfaino.diapertracker

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 📦 备份与恢复编解码器 (Backup Codec)
 * 负责全量资产与分类的 JSON 序列化导出与安全反序列化导入。
 * 作为 DataStore 门面下沉的专用组件，严格保持 JSON 字段协议 100% 向后兼容。
 */
object BackupCodec {

    private const val TAG = "BackupCodec"

    fun exportBackupJson(
        categories: List<String>,
        entries: List<Entry>
    ): String {
        val root = JSONObject()
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        val catArr = JSONArray()
        for (c in categories) catArr.put(c)
        root.put("categories", catArr)

        val entryArr = JSONArray()
        for (e in entries) {
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
                    .put("img_p", e.photoPath)
                    .put("rec_p", e.receiptPath)
            )
        }
        root.put("entries", entryArr)
        return root.toString(2)
    }

    fun importBackupJson(
        jsonStr: String,
        getCategories: () -> List<String>,
        saveCategories: (List<String>) -> Unit,
        saveEntries: (List<Entry>) -> Unit
    ): Boolean {
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
                            id = o.optString("id", UUID.randomUUID().toString()),
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
                            expiryDate = o.optLong("e_date", 0L),
                            photoPath = o.optString("img_p", ""),
                            receiptPath = o.optString("rec_p", "")
                        )
                    )
                }
                saveEntries(list)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "importBackupJson 导入失败", e)
            false
        }
    }
}
