package com.kfaino.diapertracker

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 📦 物品资产核心 CRUD 与时光流转持久化仓储 (Entry Repository)
 * 封装物品出入库记录、折旧、退役流转、重要度、订阅周期、时光回忆与借还台账。
 * 作为 DataStore 门面下沉的底层仓储，严格保持 SharedPreferences Key 完全不变。
 */
class EntryRepository(private val prefs: SharedPreferences) {

    companion object {
        private const val TAG = "EntryRepository"
        const val KEY_LAST_USED_UNIT = "last_used_unit"
    }

    fun loadAll(keyEntries: String): List<Entry> {
        val raw = prefs.getString(keyEntries, null)
            ?: prefs.getString("entries_v3", null)
            ?: prefs.getString("entries_v2", null)
            ?: return emptyList()

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

                val momentsArr = o.optJSONArray("moments")
                val momentsList = mutableListOf<ItemMemoryMoment>()
                if (momentsArr != null) {
                    for (m in 0 until momentsArr.length()) {
                        val mo = momentsArr.getJSONObject(m)
                        momentsList.add(
                            ItemMemoryMoment(
                                id = mo.optString("id", UUID.randomUUID().toString()),
                                title = mo.optString("title", ""),
                                story = mo.optString("story", ""),
                                photoPath = mo.optString("photo", ""),
                                date = mo.optLong("date", System.currentTimeMillis()),
                                moodEmoji = mo.optString("emoji", "✨"),
                                rating = mo.optInt("rating", 5)
                            )
                        )
                    }
                }

                val lendingArr = o.optJSONArray("lending_hist")
                val lendingList = mutableListOf<LendingRecord>()
                if (lendingArr != null) {
                    for (l in 0 until lendingArr.length()) {
                        val lo = lendingArr.getJSONObject(l)
                        lendingList.add(
                            LendingRecord(
                                id = lo.optString("id", UUID.randomUUID().toString()),
                                borrowerName = lo.optString("b_name", ""),
                                borrowerContact = lo.optString("b_contact", ""),
                                lentDate = lo.optLong("l_date", System.currentTimeMillis()),
                                expectedReturnDate = lo.optLong("exp_date", 0L),
                                actualReturnDate = lo.optLong("act_date", 0L),
                                deposit = lo.optDouble("dep", 0.0),
                                notes = lo.optString("notes", ""),
                                photoPath = lo.optString("photo", ""),
                                status = lo.optString("status", "lent"),
                                returnConditionRating = lo.optInt("rating", 5)
                            )
                        )
                    }
                }

                result.add(
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
                        receiptPath = o.optString("rec_p", ""),
                        minStockThreshold = o.optInt("min_th", 0),
                        isDigital = o.optBoolean("is_dig", false),
                        digitalType = o.optString("dig_type", "album"),
                        digitalUrl = o.optString("dig_url", ""),
                        digitalSize = o.optString("dig_sz", ""),
                        digitalLicenseKey = o.optString("dig_key", ""),
                        backupStatus = o.optString("bak_st", "local"),
                        memoryMoments = momentsList,
                        isLentOut = o.optBoolean("is_lent", false),
                        currentBorrower = o.optString("c_borrower", ""),
                        currentBorrowerContact = o.optString("c_contact", ""),
                        currentLentDate = o.optLong("c_lent_ts", 0L),
                        currentDeposit = o.optDouble("c_dep", 0.0),
                        lendingHistory = lendingList,
                        maintenanceIntervalMonths = o.optInt("maint_int", 0),
                        lastMaintainedAt = o.optLong("maint_ts", 0L),
                        maintenanceNotes = o.optString("maint_note", "")
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "loadAll 解析失败", e)
            emptyList()
        }
    }

    fun saveAll(entries: List<Entry>, keyEntries: String, onSaved: () -> Unit) {
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

            val momentsArr = JSONArray()
            for (m in e.memoryMoments) {
                momentsArr.put(
                    JSONObject()
                        .put("id", m.id)
                        .put("title", m.title)
                        .put("story", m.story)
                        .put("photo", m.photoPath)
                        .put("date", m.date)
                        .put("emoji", m.moodEmoji)
                        .put("rating", m.rating)
                )
            }

            val lendingArr = JSONArray()
            for (l in e.lendingHistory) {
                lendingArr.put(
                    JSONObject()
                        .put("id", l.id)
                        .put("b_name", l.borrowerName)
                        .put("b_contact", l.borrowerContact)
                        .put("l_date", l.lentDate)
                        .put("exp_date", l.expectedReturnDate)
                        .put("act_date", l.actualReturnDate)
                        .put("dep", l.deposit)
                        .put("notes", l.notes)
                        .put("photo", l.photoPath)
                        .put("status", l.status)
                        .put("rating", l.returnConditionRating)
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
                    .put("img_p", e.photoPath)
                    .put("rec_p", e.receiptPath)
                    .put("min_th", e.minStockThreshold)
                    .put("is_dig", e.isDigital)
                    .put("dig_type", e.digitalType)
                    .put("dig_url", e.digitalUrl)
                    .put("dig_sz", e.digitalSize)
                    .put("dig_key", e.digitalLicenseKey)
                    .put("bak_st", e.backupStatus)
                    .put("moments", momentsArr)
                    .put("is_lent", e.isLentOut)
                    .put("c_borrower", e.currentBorrower)
                    .put("c_contact", e.currentBorrowerContact)
                    .put("c_lent_ts", e.currentLentDate)
                    .put("exp_ret_ts", e.expectedReturnDate)
                    .put("c_dep", e.currentDeposit)
                    .put("lending_hist", lendingArr)
                    .put("maint_int", e.maintenanceIntervalMonths)
                    .put("maint_ts", e.lastMaintainedAt)
                    .put("maint_note", e.maintenanceNotes)
            )
        }
        prefs.edit().putString(keyEntries, arr.toString()).apply()
        onSaved()
    }

    fun updateEntry(index: Int, newEntry: Entry, keyEntries: String, onSaved: () -> Unit): Boolean {
        val list = loadAll(keyEntries).toMutableList()
        if (index in 0 until list.size) {
            val oldEntry = list[index]
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
            saveAll(list, keyEntries, onSaved)
            return true
        }
        return false
    }

    fun deleteEntryAt(index: Int, keyEntries: String, onSaved: () -> Unit): Boolean {
        val list = loadAll(keyEntries).toMutableList()
        if (index in 0 until list.size) {
            list.removeAt(index)
            saveAll(list, keyEntries, onSaved)
            return true
        }
        return false
    }

    fun setRetired(
        entryId: String,
        isRetired: Boolean,
        action: String,
        soldPrice: Double,
        note: String,
        keyEntries: String,
        onSaved: () -> Unit
    ) {
        val list = loadAll(keyEntries).toMutableList()
        val idx = list.indexOfFirst { it.id == entryId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isRetired = isRetired,
                retiredAt = if (isRetired) System.currentTimeMillis() else 0L,
                retiredAction = if (isRetired) action else "",
                retiredSoldPrice = soldPrice,
                retiredNote = note
            )
            saveAll(list, keyEntries, onSaved)
        }
    }

    fun clearAllData(keyEntries: String) {
        prefs.edit().remove(keyEntries).apply()
    }

    fun getLastUsedUnit(): String = prefs.getString(KEY_LAST_USED_UNIT, "件") ?: "件"

    fun setLastUsedUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_LAST_USED_UNIT, trimmed).apply()
        }
    }

    fun confirmItemChecked(entryId: String, keyEntries: String, onSaved: () -> Unit) {
        val list = loadAll(keyEntries).toMutableList()
        val idx = list.indexOfFirst { it.id == entryId }
        if (idx != -1) {
            list[idx] = list[idx].copy(lastCheckedAt = System.currentTimeMillis())
            saveAll(list, keyEntries, onSaved)
        }
    }

    fun addMemoryMoment(entryId: String, moment: ItemMemoryMoment, keyEntries: String, onSaved: () -> Unit): Boolean {
        val all = loadAll(keyEntries).toMutableList()
        val idx = all.indexOfFirst { it.id == entryId }
        if (idx == -1) return false
        val entry = all[idx]
        val newMoments = (entry.memoryMoments + moment).sortedByDescending { it.date }
        all[idx] = entry.copy(memoryMoments = newMoments)
        saveAll(all, keyEntries, onSaved)
        return true
    }

    fun updateMemoryMoment(entryId: String, moment: ItemMemoryMoment, keyEntries: String, onSaved: () -> Unit): Boolean {
        val all = loadAll(keyEntries).toMutableList()
        val idx = all.indexOfFirst { it.id == entryId }
        if (idx == -1) return false
        val entry = all[idx]
        val newMoments = entry.memoryMoments.map { if (it.id == moment.id) moment else it }.sortedByDescending { it.date }
        all[idx] = entry.copy(memoryMoments = newMoments)
        saveAll(all, keyEntries, onSaved)
        return true
    }

    fun deleteMemoryMoment(entryId: String, momentId: String, keyEntries: String, onSaved: () -> Unit): Boolean {
        val all = loadAll(keyEntries).toMutableList()
        val idx = all.indexOfFirst { it.id == entryId }
        if (idx == -1) return false
        val entry = all[idx]
        val newMoments = entry.memoryMoments.filter { it.id != momentId }
        all[idx] = entry.copy(memoryMoments = newMoments)
        saveAll(all, keyEntries, onSaved)
        return true
    }

    fun getAllBorrowerNames(keyEntries: String): List<String> {
        val names = mutableSetOf<String>()
        for (e in loadAll(keyEntries)) {
            if (e.currentBorrower.isNotBlank()) names.add(e.currentBorrower)
            for (r in e.lendingHistory) {
                if (r.borrowerName.isNotBlank()) names.add(r.borrowerName)
            }
        }
        return names.toList()
    }

    fun lendAsset(
        entryId: String,
        borrowerName: String,
        borrowerContact: String,
        expectedReturnDate: Long,
        deposit: Double,
        notes: String,
        photoPath: String,
        keyEntries: String,
        onSaved: () -> Unit
    ): Boolean {
        val all = loadAll(keyEntries).toMutableList()
        val idx = all.indexOfFirst { it.id == entryId }
        if (idx == -1) return false
        val entry = all[idx]

        val newRecord = LendingRecord(
            id = UUID.randomUUID().toString(),
            borrowerName = borrowerName,
            borrowerContact = borrowerContact,
            lentDate = System.currentTimeMillis(),
            expectedReturnDate = expectedReturnDate,
            actualReturnDate = 0L,
            deposit = deposit,
            notes = notes,
            photoPath = photoPath,
            status = "lent"
        )

        all[idx] = entry.copy(
            isLentOut = true,
            currentBorrower = borrowerName,
            currentBorrowerContact = borrowerContact,
            currentLentDate = System.currentTimeMillis(),
            expectedReturnDate = expectedReturnDate,
            currentDeposit = deposit,
            lendingHistory = listOf(newRecord) + entry.lendingHistory
        )
        saveAll(all, keyEntries, onSaved)
        return true
    }

    fun returnAsset(
        entryId: String,
        actualReturnDate: Long,
        returnConditionRating: Int,
        notes: String,
        keyEntries: String,
        onSaved: () -> Unit
    ): Boolean {
        val all = loadAll(keyEntries).toMutableList()
        val idx = all.indexOfFirst { it.id == entryId }
        if (idx == -1) return false
        val entry = all[idx]

        val updatedHistory = entry.lendingHistory.mapIndexed { index, record ->
            if (index == 0 && (record.status == "lent" || record.actualReturnDate == 0L)) {
                record.copy(
                    actualReturnDate = actualReturnDate,
                    status = "returned",
                    returnConditionRating = returnConditionRating,
                    notes = if (notes.isNotBlank()) "${record.notes}\n[归还备注] $notes".trim() else record.notes
                )
            } else {
                record
            }
        }

        all[idx] = entry.copy(
            isLentOut = false,
            currentBorrower = "",
            currentBorrowerContact = "",
            currentLentDate = 0L,
            expectedReturnDate = 0L,
            currentDeposit = 0.0,
            lendingHistory = updatedHistory
        )
        saveAll(all, keyEntries, onSaved)
        return true
    }
}
