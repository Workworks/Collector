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
        prefs.edit().putString(keyVouchers, arr.toString()).apply()
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
        prefs.edit().putString(keyIdentityDocs, arr.toString()).apply()
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
        prefs.edit().putString(keyMedicines, arr.toString()).apply()
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
        prefs.edit().putString(keyFoods, arr.toString()).apply()
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
        prefs.edit().putString(keyHonors, arr.toString()).apply()
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

/** 06. 👗 换季衣橱仓储 */
internal class WardrobeVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_wardrobe_v1"

    fun getRecords(): List<WardrobeRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<WardrobeRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WardrobeRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        season = o.optString("season", "all"),
                        category = o.optString("category", "top"),
                        color = o.optString("color", ""),
                        material = o.optString("material", ""),
                        storageLocation = o.optString("loc", "主卧衣柜"),
                        purchasePrice = o.optDouble("price", 0.0),
                        purchaseDate = o.optLong("p_date", System.currentTimeMillis()),
                        photoPath = o.optString("photo", ""),
                        wearCount = o.optInt("w_cnt", 0),
                        lastWornAt = o.optLong("w_at", 0L),
                        isSealed = o.optBoolean("sealed", false),
                        sealedAt = o.optLong("s_at", 0L),
                        careNotes = o.optString("care", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<WardrobeRecord>) {
        val arr = JSONArray()
        for (w in list) {
            arr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("name", w.name)
                    .put("season", w.season)
                    .put("category", w.category)
                    .put("color", w.color)
                    .put("material", w.material)
                    .put("loc", w.storageLocation)
                    .put("price", w.purchasePrice)
                    .put("p_date", w.purchaseDate)
                    .put("photo", w.photoPath)
                    .put("w_cnt", w.wearCount)
                    .put("w_at", w.lastWornAt)
                    .put("sealed", w.isSealed)
                    .put("s_at", w.sealedAt)
                    .put("care", w.careNotes)
                    .put("notes", w.notes)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: WardrobeRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 07. 🚨 应急物资仓储 */
internal class EmergencyVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_emergency_v1"

    fun getRecords(): List<EmergencyItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<EmergencyItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    EmergencyItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        kitType = o.optString("k_type", "earthquake"),
                        category = o.optString("cat", "food"),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", "玄关应急包"),
                        expiryDate = o.optLong("exp_d", 0L),
                        lastTestedAt = o.optLong("test_at", 0L),
                        lastCheckedAt = o.optLong("chk_at", o.optLong("test_at", 0L)),
                        rotationIntervalMonths = o.optInt("rot_m", 0),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", ""),
                        importanceLevel = o.optString("imp", "must_have")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<EmergencyItem>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("k_type", e.kitType)
                    .put("cat", e.category)
                    .put("qty", e.qty)
                    .put("unit", e.unit)
                    .put("loc", e.location)
                    .put("exp_d", e.expiryDate)
                    .put("test_at", e.lastTestedAt)
                    .put("chk_at", e.lastCheckedAt)
                    .put("rot_m", e.rotationIntervalMonths)
                    .put("photo", e.photoPath)
                    .put("notes", e.notes)
                    .put("imp", e.importanceLevel)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: EmergencyItem) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 08. 🔧 工具设备维保仓储 */
internal class ToolVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_tools_v1"

    fun getRecords(): List<ToolMaintenanceRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ToolMaintenanceRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ToolMaintenanceRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        spec = o.optString("spec", ""),
                        category = o.optString("cat", "electric"),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", "工具箱"),
                        maintenanceIntervalDays = o.optInt("m_days", 0),
                        lastMaintainedAt = o.optLong("m_at", 0L),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<ToolMaintenanceRecord>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("spec", t.spec)
                    .put("cat", t.category)
                    .put("qty", t.qty)
                    .put("unit", t.unit)
                    .put("loc", t.location)
                    .put("m_days", t.maintenanceIntervalDays)
                    .put("m_at", t.lastMaintainedAt)
                    .put("photo", t.photoPath)
                    .put("notes", t.notes)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: ToolMaintenanceRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 09. 🪴 绿植花卉水肥养护仓储 */
internal class PlantVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_plants_v1"

    fun getRecords(): List<PlantCareRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
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
                        lightDemand = o.optString("light", "scattered"),
                        location = o.optString("loc", "客厅阳台"),
                        waterIntervalDays = o.optInt("w_days", 7),
                        lastWateredAt = o.optLong("w_at", 0L),
                        fertilizeIntervalDays = o.optInt("f_days", 30),
                        lastFertilizedAt = o.optLong("f_at", 0L),
                        photoPath = o.optString("photo", ""),
                        careTips = o.optString("tips", ""),
                        plantedAt = o.optLong("p_at", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<PlantCareRecord>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("species", p.species)
                    .put("light", p.lightDemand)
                    .put("loc", p.location)
                    .put("w_days", p.waterIntervalDays)
                    .put("w_at", p.lastWateredAt)
                    .put("f_days", p.fertilizeIntervalDays)
                    .put("f_at", p.lastFertilizedAt)
                    .put("photo", p.photoPath)
                    .put("tips", p.careTips)
                    .put("p_at", p.plantedAt)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: PlantCareRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 10. 🐾 萌宠生活与健康档案仓储 */
internal class PetVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_pets_v1"

    fun getRecords(): List<PetCareRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PetCareRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PetCareRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        species = o.optString("species", "cat"),
                        breed = o.optString("breed", ""),
                        weightKg = o.optDouble("weight", 0.0),
                        chipNumber = o.optString("chip", ""),
                        foodBrand = o.optString("food", ""),
                        foodStorageLocation = o.optString("f_loc", ""),
                        dewormIntervalDays = o.optInt("dw_days", 30),
                        lastDewormedAt = o.optLong("dw_at", 0L),
                        vaccineIntervalDays = o.optInt("vac_days", 365),
                        lastVaccinatedAt = o.optLong("vac_at", 0L),
                        photoPath = o.optString("photo", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<PetCareRecord>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("species", p.species)
                    .put("breed", p.breed)
                    .put("weight", p.weightKg)
                    .put("chip", p.chipNumber)
                    .put("food", p.foodBrand)
                    .put("f_loc", p.foodStorageLocation)
                    .put("dw_days", p.dewormIntervalDays)
                    .put("dw_at", p.lastDewormedAt)
                    .put("vac_days", p.vaccineIntervalDays)
                    .put("vac_at", p.lastVaccinatedAt)
                    .put("photo", p.photoPath)
                    .put("notes", p.notes)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: PetCareRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 11. 📚 书房藏书与阅读仓储 */
internal class BookVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_books_v1"

    fun getRecords(): List<BookRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
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
                        category = o.optString("cat", "general"),
                        totalPages = o.optInt("tot_p", 0),
                        currentPages = o.optInt("cur_p", 0),
                        bookshelfLocation = o.optString("loc", "书房书架"),
                        rating = o.optDouble("rating", 5.0).toFloat(),
                        borrowerName = o.optString("borrower", ""),
                        lentDate = o.optLong("lent_d", 0L),
                        summaryNotes = o.optString("notes", ""),
                        coverPhotoPath = o.optString("photo", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<BookRecord>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("author", b.author)
                    .put("cat", b.category)
                    .put("tot_p", b.totalPages)
                    .put("cur_p", b.currentPages)
                    .put("loc", b.bookshelfLocation)
                    .put("rating", b.rating.toDouble())
                    .put("borrower", b.borrowerName)
                    .put("lent_d", b.lentDate)
                    .put("notes", b.summaryNotes)
                    .put("photo", b.coverPhotoPath)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: BookRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 12. 🍷 家庭茶窖与名酿适饮仓储 */
internal class BeverageTeaVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_beverage_v1"

    fun getRecords(): List<BeverageTeaRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BeverageTeaRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    BeverageTeaRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        category = o.optString("cat", "liquor"),
                        vintageYear = o.optInt("vintage", 0),
                        optimalAgingYear = o.optInt("aging", 0),
                        qty = o.optDouble("qty", 1.0),
                        unit = o.optString("unit", "瓶"),
                        storageLocation = o.optString("loc", "恒温酒柜"),
                        originRegion = o.optString("origin", ""),
                        rating = o.optDouble("rating", 5.0).toFloat(),
                        isOpened = o.optBoolean("opened", false),
                        openedAt = o.optLong("open_at", 0L),
                        openedPreserveDays = o.optInt("p_days", 0),
                        photoPath = o.optString("photo", ""),
                        tastingNotes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(list: List<BeverageTeaRecord>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("cat", b.category)
                    .put("vintage", b.vintageYear)
                    .put("aging", b.optimalAgingYear)
                    .put("qty", b.qty)
                    .put("unit", b.unit)
                    .put("loc", b.storageLocation)
                    .put("origin", b.originRegion)
                    .put("rating", b.rating.toDouble())
                    .put("opened", b.isOpened)
                    .put("open_at", b.openedAt)
                    .put("p_days", b.openedPreserveDays)
                    .put("photo", b.photoPath)
                    .put("notes", b.tastingNotes)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: BeverageTeaRecord) {
        val list = getRecords().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveRecords(list)
    }

    fun delete(id: String) {
        saveRecords(getRecords().filter { it.id != id })
    }
}

/** 💡 13. 灵感想法舱仓储 */
internal class IdeaVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_ideas_v1"

    fun getIdeas(): List<IdeaRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IdeaRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tagsArr = o.optJSONArray("tags") ?: JSONArray()
                val tagsList = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                val linksArr = o.optJSONArray("links") ?: JSONArray()
                val linksList = (0 until linksArr.length()).map { linksArr.getString(it) }

                list.add(
                    IdeaRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        content = o.optString("content", ""),
                        tags = tagsList,
                        moodEmoji = o.optString("emoji", "💡"),
                        isPinned = o.optBoolean("pinned", false),
                        colorHex = o.optString("color", "#10B981"),
                        linkedAssetIds = linksList,
                        createdAt = o.optLong("c_at", System.currentTimeMillis()),
                        updatedAt = o.optLong("u_at", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveIdeas(list: List<IdeaRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }
            val linksArr = JSONArray()
            item.linkedAssetIds.forEach { linksArr.put(it) }

            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("content", item.content)
                    .put("tags", tagsArr)
                    .put("emoji", item.moodEmoji)
                    .put("pinned", item.isPinned)
                    .put("color", item.colorHex)
                    .put("links", linksArr)
                    .put("c_at", item.createdAt)
                    .put("u_at", item.updatedAt)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(idea: IdeaRecord) {
        val list = getIdeas().toMutableList()
        val idx = list.indexOfFirst { it.id == idea.id }
        if (idx != -1) list[idx] = idea else list.add(0, idea)
        saveIdeas(list)
    }

    fun delete(id: String) {
        saveIdeas(getIdeas().filter { it.id != id })
    }
}

/** 📰 14. 智能剪藏知识库仓储 */
internal class ClippingVaultRepository(private val prefs: SharedPreferences) {
    private val key = "vault_clippings_v1"

    fun getClippings(): List<ClippingRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ClippingRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val imgsArr = o.optJSONArray("imgs") ?: JSONArray()
                val imgsList = (0 until imgsArr.length()).map { imgsArr.getString(it) }
                val tagsArr = o.optJSONArray("tags") ?: JSONArray()
                val tagsList = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                val linksArr = o.optJSONArray("links") ?: JSONArray()
                val linksList = (0 until linksArr.length()).map { linksArr.getString(it) }

                list.add(
                    ClippingRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", ""),
                        originalUrl = o.optString("url", ""),
                        sourcePlatform = o.optString("platform", "web"),
                        fullMarkdown = o.optString("markdown", ""),
                        ocrRawText = o.optString("ocr", ""),
                        localImagePaths = imgsList,
                        summary = o.optString("summary", ""),
                        tags = tagsList,
                        linkedAssetIds = linksList,
                        isArchived = o.optBoolean("archived", false),
                        capturedAt = o.optLong("cap_at", System.currentTimeMillis()),
                        readProgressPercent = o.optInt("prog", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveClippings(list: List<ClippingRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val imgsArr = JSONArray()
            item.localImagePaths.forEach { imgsArr.put(it) }
            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }
            val linksArr = JSONArray()
            item.linkedAssetIds.forEach { linksArr.put(it) }

            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("url", item.originalUrl)
                    .put("platform", item.sourcePlatform)
                    .put("markdown", item.fullMarkdown)
                    .put("ocr", item.ocrRawText)
                    .put("imgs", imgsArr)
                    .put("summary", item.summary)
                    .put("tags", tagsArr)
                    .put("links", linksArr)
                    .put("archived", item.isArchived)
                    .put("cap_at", item.capturedAt)
                    .put("prog", item.readProgressPercent)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun addOrUpdate(record: ClippingRecord) {
        val list = getClippings().toMutableList()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx != -1) list[idx] = record else list.add(0, record)
        saveClippings(list)
    }

    fun delete(id: String) {
        saveClippings(getClippings().filter { it.id != id })
    }
}

