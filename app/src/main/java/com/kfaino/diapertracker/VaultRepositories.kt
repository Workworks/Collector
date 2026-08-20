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
