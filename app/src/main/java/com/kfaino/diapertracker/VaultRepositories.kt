package com.kfaino.diapertracker

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 📦 第一性原理收纳馆持久化仓储集 (Vault Repositories)
 *
 * 这些类是从 `DataStore` 中原样拆分出来的 —— 方法体逐字未改，只是从"上帝对象里的一段"
 * 变成了"各自独立、职责单一的类"。`DataStore` 仍然是唯一对外门面，全项目调用方无需改动。
 *
 * 拆分动机见 `docs/TECH_DEBT_AUDIT.md` P3-2。
 */

/**
 * 🎟️ 时效权益与卡券票据收纳馆 (Voucher & Privilege Vault)
 *
 * 由 [DataStore] 门面持有并委托，公开方法名与签名与拆分前完全一致。
 * ⚠️ 存储 key 与 JSON 字段名**禁止修改**，改动会导致老用户数据读不出来（见 GEMINI.md §6）。
 */
internal class VoucherVaultRepository(private val prefs: SharedPreferences) {

    // 🎟️ 第一性原理收纳：时效权益与卡券票据收纳馆 (Voucher & Privilege Vault)
    // =========================================================================

    private val keyVouchers = "vault_vouchers_v1"

    fun getVouchers(): List<VoucherRecord> {
        val raw = prefs.getString(keyVouchers, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<VoucherRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    VoucherRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", ""),
                        type = o.optString("type", "coupon"),
                        valueAmount = o.optDouble("val", 0.0),
                        minSpend = o.optDouble("min", 0.0),
                        remainingTimes = o.optInt("rem_t", 1),
                        totalTimes = o.optInt("tot_t", 1),
                        startDate = o.optLong("s_date", System.currentTimeMillis()),
                        expiryDate = o.optLong("e_date", 0L),
                        code = o.optString("code", ""),
                        platform = o.optString("plat", ""),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", ""),
                        isUsed = o.optBoolean("used", false),
                        usedAt = o.optLong("used_at", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveVouchers(list: List<VoucherRecord>) {
        val arr = JSONArray()
        for (v in list) {
            arr.put(
                JSONObject()
                    .put("id", v.id)
                    .put("title", v.title)
                    .put("type", v.type)
                    .put("val", v.valueAmount)
                    .put("min", v.minSpend)
                    .put("rem_t", v.remainingTimes)
                    .put("tot_t", v.totalTimes)
                    .put("s_date", v.startDate)
                    .put("e_date", v.expiryDate)
                    .put("code", v.code)
                    .put("plat", v.platform)
                    .put("photo", v.photoPath)
                    .put("notes", v.notes)
                    .put("used", v.isUsed)
                    .put("used_at", v.usedAt)
            )
        }
        JsonCollectionWriter.save(prefs, keyVouchers, arr)
    }

    fun addOrUpdateVoucher(voucher: VoucherRecord) {
        val list = getVouchers().toMutableList()
        val idx = list.indexOfFirst { it.id == voucher.id }
        if (idx != -1) {
            list[idx] = voucher
        } else {
            list.add(0, voucher)
        }
        saveVouchers(list)
    }

    fun deleteVoucher(voucherId: String) {
        val list = getVouchers().filter { it.id != voucherId }
        saveVouchers(list)
    }

    /** 次卡一键减扣 1 次 */
    fun useTimesCardOneTime(voucherId: String): Boolean {
        val list = getVouchers().toMutableList()
        val idx = list.indexOfFirst { it.id == voucherId }
        if (idx == -1) return false
        val card = list[idx]
        val newRemaining = (card.remainingTimes - 1).coerceAtLeast(0)
        val isNowUsed = newRemaining == 0
        list[idx] = card.copy(
            remainingTimes = newRemaining,
            isUsed = isNowUsed,
            usedAt = if (isNowUsed) System.currentTimeMillis() else card.usedAt
        )
        saveVouchers(list)
        return true
    }

    /** 标记卡券已核销/已使用 */
    fun markVoucherUsed(voucherId: String, used: Boolean) {
        val list = getVouchers().toMutableList()
        val idx = list.indexOfFirst { it.id == voucherId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isUsed = used,
                usedAt = if (used) System.currentTimeMillis() else 0L
            )
            saveVouchers(list)
        }
    }
}

/**
 * 🪪 家庭多成员证照与敏感凭证 (Family Identity & Safe)
 *
 * 由 [DataStore] 门面持有并委托，公开方法名与签名与拆分前完全一致。
 * ⚠️ 存储 key 与 JSON 字段名**禁止修改**，改动会导致老用户数据读不出来（见 GEMINI.md §6）。
 */
internal class IdentityVaultRepository(private val prefs: SharedPreferences) {

    // 🪪 第一性原理收纳：家庭多成员证照与敏感凭证 (Family Identity & Safe)
    // =========================================================================

    private val keyIdentityDocs = "vault_identity_docs_v1"

    fun getIdentityDocs(): List<IdentityDocument> {
        val raw = prefs.getString(keyIdentityDocs, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IdentityDocument>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    IdentityDocument(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        member = o.optString("mem", "本人"),
                        docType = o.optString("dtype", "id_card"),
                        docNumber = o.optString("dnum", ""),
                        nameOnDoc = o.optString("name", ""),
                        issueDate = o.optLong("iss_d", 0L),
                        expiryDate = o.optLong("exp_d", 0L),
                        frontPhotoPath = o.optString("f_photo", ""),
                        backPhotoPath = o.optString("b_photo", ""),
                        issuingAuthority = o.optString("auth", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveIdentityDocs(list: List<IdentityDocument>) {
        val arr = JSONArray()
        for (d in list) {
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("mem", d.member)
                    .put("dtype", d.docType)
                    .put("dnum", d.docNumber)
                    .put("name", d.nameOnDoc)
                    .put("iss_d", d.issueDate)
                    .put("exp_d", d.expiryDate)
                    .put("f_photo", d.frontPhotoPath)
                    .put("b_photo", d.backPhotoPath)
                    .put("auth", d.issuingAuthority)
                    .put("notes", d.notes)
            )
        }
        JsonCollectionWriter.save(prefs, keyIdentityDocs, arr)
    }

    fun addOrUpdateIdentityDoc(doc: IdentityDocument) {
        val list = getIdentityDocs().toMutableList()
        val idx = list.indexOfFirst { it.id == doc.id }
        if (idx != -1) {
            list[idx] = doc
        } else {
            list.add(0, doc)
        }
        saveIdentityDocs(list)
    }

    fun deleteIdentityDoc(docId: String) {
        val list = getIdentityDocs().filter { it.id != docId }
        saveIdentityDocs(list)
    }
}

/**
 * 💊 家庭智能健康药箱 (Medicine & Scenario Vault)
 *
 * 由 [DataStore] 门面持有并委托，公开方法名与签名与拆分前完全一致。
 * ⚠️ 存储 key 与 JSON 字段名**禁止修改**，改动会导致老用户数据读不出来（见 GEMINI.md §6）。
 */
internal class MedicineVaultRepository(private val prefs: SharedPreferences) {

    // 💊 第一性原理收纳：家庭智能健康药箱 (Medicine & Scenario Vault)
    // =========================================================================

    private val keyMedicines = "vault_medicines_v1"

    fun getMedicines(): List<MedicineRecord> {
        val raw = prefs.getString(keyMedicines, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MedicineRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MedicineRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        category = o.optString("cat", "fever"),
                        form = o.optString("form", "片剂"),
                        qty = o.optInt("qty", 1),
                        unit = o.optString("unit", "盒"),
                        location = o.optString("loc", "家庭急救药箱"),
                        dosage = o.optString("dos", ""),
                        targetAudience = o.optString("aud", "全家通用"),
                        expiryDate = o.optLong("e_date", 0L),
                        isOpened = o.optBoolean("opened", false),
                        openedAt = o.optLong("o_date", 0L),
                        openedValidityDays = o.optInt("o_days", 0),
                        photoPath = o.optString("photo", ""),
                        contraindications = o.optString("contra", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMedicines(list: List<MedicineRecord>) {
        val arr = JSONArray()
        for (m in list) {
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("cat", m.category)
                    .put("form", m.form)
                    .put("qty", m.qty)
                    .put("unit", m.unit)
                    .put("loc", m.location)
                    .put("dos", m.dosage)
                    .put("aud", m.targetAudience)
                    .put("e_date", m.expiryDate)
                    .put("opened", m.isOpened)
                    .put("o_date", m.openedAt)
                    .put("o_days", m.openedValidityDays)
                    .put("photo", m.photoPath)
                    .put("contra", m.contraindications)
            )
        }
        JsonCollectionWriter.save(prefs, keyMedicines, arr)
    }

    fun addOrUpdateMedicine(medicine: MedicineRecord) {
        val list = getMedicines().toMutableList()
        val idx = list.indexOfFirst { it.id == medicine.id }
        if (idx != -1) {
            list[idx] = medicine
        } else {
            list.add(0, medicine)
        }
        saveMedicines(list)
    }

    fun deleteMedicine(medicineId: String) {
        val list = getMedicines().filter { it.id != medicineId }
        saveMedicines(list)
    }

    /** 药品开封打卡 */
    fun markMedicineOpened(medicineId: String) {
        val list = getMedicines().toMutableList()
        val idx = list.indexOfFirst { it.id == medicineId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isOpened = true,
                openedAt = System.currentTimeMillis()
            )
            saveMedicines(list)
        }
    }
}

/**
 * 🥦 冰箱冷冻与食材生鲜鲜度库 (Food & Fresh Vault)
 *
 * 由 [DataStore] 门面持有并委托，公开方法名与签名与拆分前完全一致。
 * ⚠️ 存储 key 与 JSON 字段名**禁止修改**，改动会导致老用户数据读不出来（见 GEMINI.md §6）。
 */
internal class FoodVaultRepository(private val prefs: SharedPreferences) {

    // 🥦 第一性原理收纳：冰箱冷冻与食材生鲜鲜度库 (Food & Fresh Vault)
    // =========================================================================

    private val keyFoods = "vault_foods_v1"

    fun getFoods(): List<FoodRecord> {
        val raw = prefs.getString(keyFoods, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FoodRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    FoodRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        zone = o.optString("zone", "freezer"),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "份"),
                        location = o.optString("loc", "冰箱冷冻二层"),
                        purchaseDate = o.optLong("p_date", System.currentTimeMillis()),
                        expiryDate = o.optLong("e_date", 0L),
                        isOpened = o.optBoolean("opened", false),
                        openedAt = o.optLong("o_date", 0L),
                        openedValidityDays = o.optInt("o_days", 0),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", ""),
                        isConsumed = o.optBoolean("consumed", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveFoods(list: List<FoodRecord>) {
        val arr = JSONArray()
        for (f in list) {
            arr.put(
                JSONObject()
                    .put("id", f.id)
                    .put("name", f.name)
                    .put("zone", f.zone)
                    .put("qty", f.qty)
                    .put("unit", f.unit)
                    .put("loc", f.location)
                    .put("p_date", f.purchaseDate)
                    .put("e_date", f.expiryDate)
                    .put("opened", f.isOpened)
                    .put("o_date", f.openedAt)
                    .put("o_days", f.openedValidityDays)
                    .put("photo", f.photoPath)
                    .put("notes", f.notes)
                    .put("consumed", f.isConsumed)
            )
        }
        JsonCollectionWriter.save(prefs, keyFoods, arr)
    }

    fun addOrUpdateFood(food: FoodRecord) {
        val list = getFoods().toMutableList()
        val idx = list.indexOfFirst { it.id == food.id }
        if (idx != -1) {
            list[idx] = food
        } else {
            list.add(0, food)
        }
        saveFoods(list)
    }

    fun deleteFood(foodId: String) {
        val list = getFoods().filter { it.id != foodId }
        saveFoods(list)
    }

    /** 食材开封保鲜打卡 */
    fun markFoodOpened(foodId: String) {
        val list = getFoods().toMutableList()
        val idx = list.indexOfFirst { it.id == foodId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isOpened = true,
                openedAt = System.currentTimeMillis()
            )
            saveFoods(list)
        }
    }

    /** 烹饪/消耗食材打卡 (扣减数量或标记吃完) */
    fun consumeFood(foodId: String, delta: Double = 1.0) {
        val list = getFoods().toMutableList()
        val idx = list.indexOfFirst { it.id == foodId }
        if (idx != -1) {
            val item = list[idx]
            val newQty = (item.qty - delta).coerceAtLeast(0.0)
            list[idx] = item.copy(
                qty = newQty,
                isConsumed = newQty <= 0.0
            )
            saveFoods(list)
        }
    }
}

/**
 * 🏆 全家成长履历与职业荣誉考级勋章馆 (Honor & Credentials)
 *
 * 由 [DataStore] 门面持有并委托，公开方法名与签名与拆分前完全一致。
 * ⚠️ 存储 key 与 JSON 字段名**禁止修改**，改动会导致老用户数据读不出来（见 GEMINI.md §6）。
 */
internal class HonorVaultRepository(private val prefs: SharedPreferences) {

    // 🏆 第一性原理收纳：全家成长履历与职业荣誉考级勋章馆 (Honor & Credentials)
    // =========================================================================

    private val keyHonors = "vault_honors_v1"

    fun getHonorCredentials(): List<HonorCredential> {
        val raw = prefs.getString(keyHonors, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<HonorCredential>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    HonorCredential(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        member = o.optString("mem", "本人"),
                        category = o.optString("cat", "career"),
                        title = o.optString("title", ""),
                        certNumber = o.optString("cnum", ""),
                        issuer = o.optString("issuer", ""),
                        issueDate = o.optLong("iss_d", 0L),
                        expiryDate = o.optLong("exp_d", 0L),
                        scoreOrLevel = o.optString("score", ""),
                        photoPath = o.optString("photo", ""),
                        verifyUrl = o.optString("vurl", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHonorCredentials(list: List<HonorCredential>) {
        val arr = JSONArray()
        for (h in list) {
            arr.put(
                JSONObject()
                    .put("id", h.id)
                    .put("mem", h.member)
                    .put("cat", h.category)
                    .put("title", h.title)
                    .put("cnum", h.certNumber)
                    .put("issuer", h.issuer)
                    .put("iss_d", h.issueDate)
                    .put("exp_d", h.expiryDate)
                    .put("score", h.scoreOrLevel)
                    .put("photo", h.photoPath)
                    .put("vurl", h.verifyUrl)
                    .put("notes", h.notes)
            )
        }
        JsonCollectionWriter.save(prefs, keyHonors, arr)
    }

    fun addOrUpdateHonorCredential(honor: HonorCredential) {
        val list = getHonorCredentials().toMutableList()
        val idx = list.indexOfFirst { it.id == honor.id }
        if (idx != -1) {
            list[idx] = honor
        } else {
            list.add(0, honor)
        }
        saveHonorCredentials(list)
    }

    fun deleteHonorCredential(honorId: String) {
        val list = getHonorCredentials().filter { it.id != honorId }
        saveHonorCredentials(list)
    }
}

/**
 * 👗 换季衣橱、四季穿搭与封箱收纳舱持久化仓储 (Wardrobe & Seasonal Closet Vault)
 */
internal class WardrobeVaultRepository(private val prefs: SharedPreferences) {

    private val keyWardrobe = "vault_wardrobe_v1"
    private val TAG = "WardrobeVaultRepo"

    fun getWardrobeRecords(): List<WardrobeRecord> {
        val raw = prefs.getString(keyWardrobe, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<WardrobeRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WardrobeRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        season = o.optString("season", "winter"),
                        category = o.optString("cat", "coat"),
                        color = o.optString("color", ""),
                        material = o.optString("mat", ""),
                        careNotes = o.optString("care", ""),
                        storageLocation = o.optString("loc", ""),
                        purchasePrice = o.optDouble("price", 0.0),
                        purchaseDate = o.optLong("p_date", 0L),
                        wearCount = o.optInt("wear_cnt", 0),
                        lastWornAt = o.optLong("last_worn", 0L),
                        isSealed = o.optBoolean("sealed", false),
                        sealedAt = o.optLong("sealed_at", 0L),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析换季衣橱记录失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveWardrobeRecords(list: List<WardrobeRecord>) {
        val arr = JSONArray()
        for (w in list) {
            arr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("name", w.name)
                    .put("season", w.season)
                    .put("cat", w.category)
                    .put("color", w.color)
                    .put("mat", w.material)
                    .put("care", w.careNotes)
                    .put("loc", w.storageLocation)
                    .put("price", w.purchasePrice)
                    .put("p_date", w.purchaseDate)
                    .put("wear_cnt", w.wearCount)
                    .put("last_worn", w.lastWornAt)
                    .put("sealed", w.isSealed)
                    .put("sealed_at", w.sealedAt)
                    .put("photo", w.photoPath)
                    .put("notes", w.notes)
            )
        }
        JsonCollectionWriter.save(prefs, keyWardrobe, arr)
    }

    fun addOrUpdateWardrobeRecord(record: WardrobeRecord) {
        val list = getWardrobeRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        saveWardrobeRecords(list)
    }

    fun deleteWardrobeRecord(recordId: String) {
        val list = getWardrobeRecords().filter { it.id != recordId }
        saveWardrobeRecords(list)
    }

    fun markWardrobeWorn(recordId: String) {
        val list = getWardrobeRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(
                wearCount = cur.wearCount + 1,
                lastWornAt = System.currentTimeMillis()
            )
            saveWardrobeRecords(list)
        }
    }

    fun toggleWardrobeSealed(recordId: String) {
        val list = getWardrobeRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            val newSealed = !cur.isSealed
            list[idx] = cur.copy(
                isSealed = newSealed,
                sealedAt = if (newSealed) System.currentTimeMillis() else 0L
            )
            saveWardrobeRecords(list)
        }
    }
}

/**
 * 🚨 家庭应急防灾、避难包与生命线物资持久化仓储 (Emergency & Survival Vault)
 */
internal class EmergencyVaultRepository(private val prefs: SharedPreferences) {

    private val keyEmergency = "vault_emergency_v1"
    private val TAG = "EmergencyVaultRepo"

    fun getEmergencyItems(): List<EmergencyItem> {
        val raw = prefs.getString(keyEmergency, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<EmergencyItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    EmergencyItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        kitType = o.optString("kit", "earthquake"),
                        category = o.optString("cat", "tool"),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", ""),
                        expiryDate = o.optLong("exp_d", 0L),
                        rotationIntervalMonths = o.optInt("rot_m", 0),
                        lastCheckedAt = o.optLong("chk_d", 0L),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析应急防灾物资失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveEmergencyItems(list: List<EmergencyItem>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("kit", e.kitType)
                    .put("cat", e.category)
                    .put("qty", e.qty)
                    .put("unit", e.unit)
                    .put("loc", e.location)
                    .put("exp_d", e.expiryDate)
                    .put("rot_m", e.rotationIntervalMonths)
                    .put("chk_d", e.lastCheckedAt)
                    .put("notes", e.notes)
                    .put("photo", e.photoPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyEmergency, arr)
    }

    fun addOrUpdateEmergencyItem(item: EmergencyItem) {
        val list = getEmergencyItems().toMutableList()
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            list[idx] = item
        } else {
            list.add(0, item)
        }
        saveEmergencyItems(list)
    }

    fun deleteEmergencyItem(itemId: String) {
        val list = getEmergencyItems().filter { it.id != itemId }
        saveEmergencyItems(list)
    }

    fun checkItem(itemId: String) {
        val list = getEmergencyItems().toMutableList()
        val idx = list.indexOfFirst { it.id == itemId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastCheckedAt = System.currentTimeMillis())
            saveEmergencyItems(list)
        }
    }
}

/**
 * 🔧 家庭工具、五金配件与设备维保耗材持久化仓储 (Tools & Maintenance Vault)
 */
internal class ToolMaintenanceVaultRepository(private val prefs: SharedPreferences) {

    private val keyTools = "vault_tools_maintenance_v1"
    private val TAG = "ToolVaultRepo"

    fun getToolRecords(): List<ToolMaintenanceRecord> {
        val raw = prefs.getString(keyTools, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ToolMaintenanceRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ToolMaintenanceRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        category = o.optString("cat", "power_tool"),
                        spec = o.optString("spec", ""),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", ""),
                        maintenanceIntervalDays = o.optInt("interval_d", 0),
                        lastMaintainedAt = o.optLong("last_maint_d", 0L),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析工具五金与维保记录失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveToolRecords(list: List<ToolMaintenanceRecord>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("cat", t.category)
                    .put("spec", t.spec)
                    .put("qty", t.qty)
                    .put("unit", t.unit)
                    .put("loc", t.location)
                    .put("interval_d", t.maintenanceIntervalDays)
                    .put("last_maint_d", t.lastMaintainedAt)
                    .put("notes", t.notes)
                    .put("photo", t.photoPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyTools, arr)
    }

    fun addOrUpdateToolRecord(record: ToolMaintenanceRecord) {
        val list = getToolRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        saveToolRecords(list)
    }

    fun deleteToolRecord(recordId: String) {
        val list = getToolRecords().filter { it.id != recordId }
        saveToolRecords(list)
    }

    fun markMaintained(recordId: String) {
        val list = getToolRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastMaintainedAt = System.currentTimeMillis())
            saveToolRecords(list)
        }
    }
}

/**
 * 🪴 家庭绿植花卉、多肉与水肥养护日历持久化仓储 (Plant Care Vault)
 */
internal class PlantCareVaultRepository(private val prefs: SharedPreferences) {

    private val keyPlants = "vault_plants_care_v1"
    private val TAG = "PlantCareRepo"

    fun getPlantRecords(): List<PlantCareRecord> {
        val raw = prefs.getString(keyPlants, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PlantCareRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PlantCareRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        species = o.optString("species", ""),
                        location = o.optString("loc", ""),
                        lightDemand = o.optString("light", "semi_shade"),
                        waterIntervalDays = o.optInt("water_d", 7),
                        lastWateredAt = o.optLong("last_water_d", 0L),
                        fertilizeIntervalDays = o.optInt("fert_d", 30),
                        lastFertilizedAt = o.optLong("last_fert_d", 0L),
                        careTips = o.optString("tips", ""),
                        photoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析绿植水肥养护记录失败: ${e.message}", e)
            emptyList()
        }
    }

    fun savePlantRecords(list: List<PlantCareRecord>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("species", p.species)
                    .put("loc", p.location)
                    .put("light", p.lightDemand)
                    .put("water_d", p.waterIntervalDays)
                    .put("last_water_d", p.lastWateredAt)
                    .put("fert_d", p.fertilizeIntervalDays)
                    .put("last_fert_d", p.lastFertilizedAt)
                    .put("tips", p.careTips)
                    .put("photo", p.photoPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyPlants, arr)
    }

    fun addOrUpdatePlantRecord(record: PlantCareRecord) {
        val list = getPlantRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        savePlantRecords(list)
    }

    fun deletePlantRecord(recordId: String) {
        val list = getPlantRecords().filter { it.id != recordId }
        savePlantRecords(list)
    }

    fun waterPlant(recordId: String) {
        val list = getPlantRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastWateredAt = System.currentTimeMillis())
            savePlantRecords(list)
        }
    }

    fun fertilizePlant(recordId: String) {
        val list = getPlantRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastFertilizedAt = System.currentTimeMillis())
            savePlantRecords(list)
        }
    }
}

/**
 * 🐾 家庭萌宠档案、疫苗驱虫与主粮耗材持久化仓储 (Pet Care Vault)
 */
internal class PetCareVaultRepository(private val prefs: SharedPreferences) {

    private val keyPets = "vault_pets_care_v1"
    private val TAG = "PetCareRepo"

    fun getPetRecords(): List<PetCareRecord> {
        val raw = prefs.getString(keyPets, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PetCareRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PetCareRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        species = o.optString("species", "猫咪"),
                        birthDate = o.optLong("birth_d", 0L),
                        weightKg = o.optDouble("weight", 0.0),
                        microchipId = o.optString("chip_id", ""),
                        dewormIntervalDays = o.optInt("deworm_d", 30),
                        lastDewormedAt = o.optLong("last_deworm_d", 0L),
                        vaccineIntervalDays = o.optInt("vax_d", 365),
                        lastVaccinatedAt = o.optLong("last_vax_d", 0L),
                        foodBrand = o.optString("food", ""),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析萌宠健康档案失败: ${e.message}", e)
            emptyList()
        }
    }

    fun savePetRecords(list: List<PetCareRecord>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("species", p.species)
                    .put("birth_d", p.birthDate)
                    .put("weight", p.weightKg)
                    .put("chip_id", p.microchipId)
                    .put("deworm_d", p.dewormIntervalDays)
                    .put("last_deworm_d", p.lastDewormedAt)
                    .put("vax_d", p.vaccineIntervalDays)
                    .put("last_vax_d", p.lastVaccinatedAt)
                    .put("food", p.foodBrand)
                    .put("notes", p.notes)
                    .put("photo", p.photoPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyPets, arr)
    }

    fun addOrUpdatePetRecord(record: PetCareRecord) {
        val list = getPetRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        savePetRecords(list)
    }

    fun deletePetRecord(recordId: String) {
        val list = getPetRecords().filter { it.id != recordId }
        savePetRecords(list)
    }

    fun markDewormed(recordId: String) {
        val list = getPetRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastDewormedAt = System.currentTimeMillis())
            savePetRecords(list)
        }
    }

    fun markVaccinated(recordId: String) {
        val list = getPetRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(lastVaccinatedAt = System.currentTimeMillis())
            savePetRecords(list)
        }
    }
}

/**
 * 📚 家庭书房藏书、借阅流转与阅读笔记持久化仓储 (Bookshelf Vault)
 */
internal class BookVaultRepository(private val prefs: SharedPreferences) {

    private val keyBooks = "vault_books_v1"
    private val TAG = "BookVaultRepo"

    fun getBookRecords(): List<BookRecord> {
        val raw = prefs.getString(keyBooks, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BookRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    BookRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", ""),
                        author = o.optString("author", ""),
                        category = o.optString("category", "社科人文"),
                        bookshelfLocation = o.optString("location", ""),
                        totalPages = o.optInt("total_p", 300),
                        currentPages = o.optInt("cur_p", 0),
                        readingStatus = o.optString("status", "unread"),
                        rating = o.optDouble("rating", 5.0).toFloat(),
                        borrowerName = o.optString("borrower", ""),
                        lentDate = o.optLong("lent_d", 0L),
                        summaryNotes = o.optString("notes", ""),
                        coverPath = o.optString("cover", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析书房藏书与阅读档案失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveBookRecords(list: List<BookRecord>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("author", b.author)
                    .put("category", b.category)
                    .put("location", b.bookshelfLocation)
                    .put("total_p", b.totalPages)
                    .put("cur_p", b.currentPages)
                    .put("status", b.readingStatus)
                    .put("rating", b.rating.toDouble())
                    .put("borrower", b.borrowerName)
                    .put("lent_d", b.lentDate)
                    .put("notes", b.summaryNotes)
                    .put("cover", b.coverPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyBooks, arr)
    }

    fun addOrUpdateBookRecord(record: BookRecord) {
        val list = getBookRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        saveBookRecords(list)
    }

    fun deleteBookRecord(recordId: String) {
        val list = getBookRecords().filter { it.id != recordId }
        saveBookRecords(list)
    }

    fun updateReadingProgress(recordId: String, currentPages: Int) {
        val list = getBookRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            val clampedPages = currentPages.coerceIn(0, cur.totalPages.coerceAtLeast(1))
            val newStatus = if (clampedPages >= cur.totalPages && cur.totalPages > 0) "finished" else "reading"
            list[idx] = cur.copy(currentPages = clampedPages, readingStatus = newStatus)
            saveBookRecords(list)
        }
    }

    fun markLent(recordId: String, borrowerName: String) {
        val list = getBookRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(
                readingStatus = "lent",
                borrowerName = borrowerName,
                lentDate = System.currentTimeMillis()
            )
            saveBookRecords(list)
        }
    }

    fun markReturned(recordId: String) {
        val list = getBookRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(
                readingStatus = "unread",
                borrowerName = "",
                lentDate = 0L
            )
            saveBookRecords(list)
        }
    }
}

/**
 * 🍷 家庭茶窖、酒品珍藏与适饮熟成时效持久化仓储 (Cellar & Tea Vault)
 */
internal class BeverageTeaVaultRepository(private val prefs: SharedPreferences) {

    private val keyBeverage = "vault_beverage_tea_v1"
    private val TAG = "BeverageVaultRepo"

    fun getBeverageRecords(): List<BeverageTeaRecord> {
        val raw = prefs.getString(keyBeverage, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BeverageTeaRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    BeverageTeaRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        category = o.optString("category", "茶品干货"),
                        vintageYear = o.optInt("vintage", 2020),
                        originRegion = o.optString("origin", ""),
                        storageLocation = o.optString("location", ""),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "瓶"),
                        openedAt = o.optLong("opened_at", 0L),
                        bestDrinkingYear = o.optInt("best_year", 2030),
                        openShelfLifeDays = o.optInt("open_life_d", 0),
                        tastingNotes = o.optString("notes", ""),
                        rating = o.optDouble("rating", 5.0).toFloat(),
                        photoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w(TAG, "解析茶品酒水档案失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveBeverageRecords(list: List<BeverageTeaRecord>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("category", b.category)
                    .put("vintage", b.vintageYear)
                    .put("origin", b.originRegion)
                    .put("location", b.storageLocation)
                    .put("qty", b.qty)
                    .put("unit", b.unit)
                    .put("opened_at", b.openedAt)
                    .put("best_year", b.bestDrinkingYear)
                    .put("open_life_d", b.openShelfLifeDays)
                    .put("notes", b.tastingNotes)
                    .put("rating", b.rating.toDouble())
                    .put("photo", b.photoPath)
            )
        }
        JsonCollectionWriter.save(prefs, keyBeverage, arr)
    }

    fun addOrUpdateBeverageRecord(record: BeverageTeaRecord) {
        val list = getBeverageRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) {
            list[idx] = record
        } else {
            list.add(0, record)
        }
        saveBeverageRecords(list)
    }

    fun deleteBeverageRecord(recordId: String) {
        val list = getBeverageRecords().filter { it.id != recordId }
        saveBeverageRecords(list)
    }

    fun openBeverage(recordId: String) {
        val list = getBeverageRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(openedAt = System.currentTimeMillis())
            saveBeverageRecords(list)
        }
    }

    fun consumeQty(recordId: String, delta: Double = 1.0) {
        val list = getBeverageRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == recordId }
        if (idx != -1) {
            val cur = list[idx]
            val newQty = (cur.qty - delta).coerceAtLeast(0.0)
            list[idx] = cur.copy(qty = newQty)
            saveBeverageRecords(list)
        }
    }
}

/**
 * 💡 闪念灵感与想法收纳仓储 (Idea & Thought Vault)
 */
internal class IdeaVaultRepository(private val prefs: SharedPreferences) {

    private val keyIdeas = "vault_ideas_v1"

    fun getIdeas(): List<IdeaRecord> {
        val raw = prefs.getString(keyIdeas, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IdeaRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tagsArr = o.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(t))
                }
                val linkedArr = o.optJSONArray("linked_ids")
                val linkedList = mutableListOf<String>()
                if (linkedArr != null) {
                    for (l in 0 until linkedArr.length()) linkedList.add(linkedArr.optString(l))
                }
                list.add(
                    IdeaRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        content = o.optString("content", ""),
                        tags = tagsList,
                        voiceMemoPath = o.optString("voice", ""),
                        moodEmoji = o.optString("emoji", "💡"),
                        isPinned = o.optBoolean("pinned", false),
                        colorHex = o.optString("color", "#10B981"),
                        linkedAssetIds = linkedList,
                        createdAt = o.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = o.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
            list.sortedWith(compareByDescending<IdeaRecord> { it.isPinned }.thenByDescending { it.updatedAt })
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveIdeas(list: List<IdeaRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
                .put("id", item.id)
                .put("content", item.content)
                .put("voice", item.voiceMemoPath)
                .put("emoji", item.moodEmoji)
                .put("pinned", item.isPinned)
                .put("color", item.colorHex)
                .put("created_at", item.createdAt)
                .put("updated_at", item.updatedAt)

            if (item.tags.isNotEmpty()) {
                val tArr = JSONArray()
                for (t in item.tags) tArr.put(t)
                obj.put("tags", tArr)
            }
            if (item.linkedAssetIds.isNotEmpty()) {
                val lArr = JSONArray()
                for (l in item.linkedAssetIds) lArr.put(l)
                obj.put("linked_ids", lArr)
            }
            arr.put(obj)
        }
        JsonCollectionWriter.save(prefs, keyIdeas, arr)
    }

    fun addOrUpdateIdea(idea: IdeaRecord) {
        val list = getIdeas().toMutableList()
        val idx = list.indexOfFirst { it.id == idea.id }
        if (idx != -1) {
            list[idx] = idea.copy(updatedAt = System.currentTimeMillis())
        } else {
            list.add(0, idea)
        }
        saveIdeas(list)
    }

    fun deleteIdea(id: String) {
        val list = getIdeas().filter { it.id != id }
        saveIdeas(list)
    }

    fun togglePin(id: String) {
        val list = getIdeas().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(isPinned = !list[idx].isPinned, updatedAt = System.currentTimeMillis())
            saveIdeas(list)
        }
    }
}

/**
 * 📰 智能截图与网络文章剪藏仓储 (Clipping & Knowledge Vault)
 */
internal class ClippingVaultRepository(private val prefs: SharedPreferences) {

    private val keyClippings = "vault_clippings_v1"

    fun getClippings(): List<ClippingRecord> {
        val raw = prefs.getString(keyClippings, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ClippingRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tagsArr = o.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(t))
                }
                val imgArr = o.optJSONArray("images")
                val imgList = mutableListOf<String>()
                if (imgArr != null) {
                    for (img in 0 until imgArr.length()) imgList.add(imgArr.optString(img))
                }
                val linkedArr = o.optJSONArray("linked_ids")
                val linkedList = mutableListOf<String>()
                if (linkedArr != null) {
                    for (l in 0 until linkedArr.length()) linkedList.add(linkedArr.optString(l))
                }
                list.add(
                    ClippingRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", ""),
                        originalUrl = o.optString("url", ""),
                        sourcePlatform = o.optString("platform", "screenshot"),
                        fullMarkdown = o.optString("markdown", ""),
                        ocrRawText = o.optString("ocr", ""),
                        localImagePaths = imgList,
                        summary = o.optString("summary", ""),
                        tags = tagsList,
                        readingProgress = o.optDouble("progress", 0.0).toFloat(),
                        isArchived = o.optBoolean("archived", false),
                        linkedAssetIds = linkedList,
                        capturedAt = o.optLong("captured_at", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.capturedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveClippings(list: List<ClippingRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("url", item.originalUrl)
                .put("platform", item.sourcePlatform)
                .put("markdown", item.fullMarkdown)
                .put("ocr", item.ocrRawText)
                .put("summary", item.summary)
                .put("progress", item.readingProgress.toDouble())
                .put("archived", item.isArchived)
                .put("captured_at", item.capturedAt)

            if (item.tags.isNotEmpty()) {
                val tArr = JSONArray()
                for (t in item.tags) tArr.put(t)
                obj.put("tags", tArr)
            }
            if (item.localImagePaths.isNotEmpty()) {
                val imgArr = JSONArray()
                for (img in item.localImagePaths) imgArr.put(img)
                obj.put("images", imgArr)
            }
            if (item.linkedAssetIds.isNotEmpty()) {
                val lArr = JSONArray()
                for (l in item.linkedAssetIds) lArr.put(l)
                obj.put("linked_ids", lArr)
            }
            arr.put(obj)
        }
        JsonCollectionWriter.save(prefs, keyClippings, arr)
    }

    fun addOrUpdateClipping(clipping: ClippingRecord) {
        val list = getClippings().toMutableList()
        val idx = list.indexOfFirst { it.id == clipping.id }
        if (idx != -1) {
            list[idx] = clipping
        } else {
            list.add(0, clipping)
        }
        saveClippings(list)
    }

    fun deleteClipping(id: String) {
        val list = getClippings().filter { it.id != id }
        saveClippings(list)
    }

    fun toggleArchive(id: String) {
        val list = getClippings().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(isArchived = !list[idx].isArchived)
            saveClippings(list)
        }
    }
}








