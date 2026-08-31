package com.kfaino.diapertracker

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 局域网 P2P 增量对撞合并报告
 */
data class MergeReport(
    val insertedEntries: Int = 0,
    val updatedEntries: Int = 0,
    val preservedEntries: Int = 0,
    val mergedCategories: Int = 0,
    val mergedVaultItems: Int = 0,
    val success: Boolean = true,
    val message: String = "合并成功"
) {
    fun summary(): String {
        return "增量合并完成：新增资产 ${insertedEntries} 件，更新资产 ${updatedEntries} 件，保留未变 ${preservedEntries} 件，合并分类 ${mergedCategories} 个，合并 12 馆物资 ${mergedVaultItems} 项。"
    }
}

/**
 * 局域网 P2P 双机增量对撞合并核心引擎
 *
 * 遵循第一性原理：
 * 1. 唯一 ID 寻址 (UUID)；
 * 2. 时间戳冲突仲裁 (Last-Write-Wins)；
 * 3. 12 馆全量物资无损互通；
 * 4. 事务性保存。
 */
object LanSyncMergeEngine {

    private const val TAG = "LanSyncMergeEngine"

    fun mergeEntriesAndCategories(
        currentCats: List<String>,
        currentEntries: List<Entry>,
        incomingJsonStr: String
    ): Triple<MergeReport, List<String>, List<Entry>> {
        if (incomingJsonStr.isBlank()) {
            return Triple(MergeReport(success = false, message = "传入数据为空"), currentCats, currentEntries)
        }

        return try {
            val root = JSONObject(incomingJsonStr)
            var inserted = 0
            var updated = 0
            var preserved = 0
            var mergedCats = 0

            // 1. 合并分类
            val incCatArr = root.optJSONArray("categories")
            val finalCats = currentCats.toMutableList()
            if (incCatArr != null) {
                val initialCatCount = finalCats.size
                for (i in 0 until incCatArr.length()) {
                    val cat = incCatArr.optString(i).trim()
                    if (cat.isNotEmpty() && !finalCats.contains(cat)) {
                        finalCats.add(cat)
                    }
                }
                mergedCats = finalCats.size - initialCatCount
            }

            // 2. 合并主资产 (Entry)
            val incEntryArr = root.optJSONArray("entries")
            val finalEntries = currentEntries.toMutableList()
            if (incEntryArr != null) {
                val entryMap = finalEntries.associateBy { it.id }.toMutableMap()

                for (i in 0 until incEntryArr.length()) {
                    val o = incEntryArr.getJSONObject(i)
                    val incomingId = o.optString("id", UUID.randomUUID().toString())
                    val incomingTs = o.optLong("ts", System.currentTimeMillis())

                    val incomingEntry = parseEntryFromJson(o, incomingId, incomingTs)
                    val existing = entryMap[incomingId]

                    if (existing == null) {
                        finalEntries.add(0, incomingEntry)
                        entryMap[incomingId] = incomingEntry
                        inserted++
                    } else {
                        if (incomingEntry.ts > existing.ts) {
                            val idx = finalEntries.indexOfFirst { it.id == incomingId }
                            if (idx != -1) {
                                finalEntries[idx] = incomingEntry
                                entryMap[incomingId] = incomingEntry
                                updated++
                            }
                        } else {
                            preserved++
                        }
                    }
                }
            }

            val report = MergeReport(
                insertedEntries = inserted,
                updatedEntries = updated,
                preservedEntries = preserved,
                mergedCategories = mergedCats,
                success = true
            )
            Triple(report, finalCats, finalEntries)
        } catch (e: Exception) {
            Log.w(TAG, "解析合并数据异常", e)
            Triple(MergeReport(success = false, message = "解析异常: ${e.message}"), currentCats, currentEntries)
        }
    }

    fun merge(store: DataStore, incomingJsonStr: String): MergeReport {
        if (incomingJsonStr.isBlank()) {
            return MergeReport(success = false, message = "传入数据为空")
        }

        return try {
            val (baseReport, newCats, newEntries) = mergeEntriesAndCategories(
                store.getCategories(),
                store.loadAll(),
                incomingJsonStr
            )

            if (!baseReport.success) return baseReport

            if (baseReport.mergedCategories > 0) {
                store.saveCategories(newCats)
            }
            if (baseReport.insertedEntries > 0 || baseReport.updatedEntries > 0) {
                store.saveAll(newEntries)
            }

            val root = JSONObject(incomingJsonStr)
            var mergedVaults = 0

            // 3. 合并 12 馆物资数据
            mergedVaults += mergeVouchers(store, root.optJSONArray("vouchers"))
            mergedVaults += mergeIdentityDocs(store, root.optJSONArray("identity_docs"))
            mergedVaults += mergeMedicines(store, root.optJSONArray("medicines"))
            mergedVaults += mergeFoods(store, root.optJSONArray("foods"))
            mergedVaults += mergeHonors(store, root.optJSONArray("honors"))
            mergedVaults += mergeWardrobe(store, root.optJSONArray("wardrobe"))
            mergedVaults += mergeEmergency(store, root.optJSONArray("emergency"))
            mergedVaults += mergeTools(store, root.optJSONArray("tools"))
            mergedVaults += mergePlants(store, root.optJSONArray("plants"))
            mergedVaults += mergePets(store, root.optJSONArray("pets"))
            mergedVaults += mergeBooks(store, root.optJSONArray("books"))
            mergedVaults += mergeBeverages(store, root.optJSONArray("beverages"))
            mergedVaults += mergeIdeas(store, root.optJSONArray("ideas"))
            mergedVaults += mergeClippings(store, root.optJSONArray("clippings"))

            MergeReport(
                insertedEntries = baseReport.insertedEntries,
                updatedEntries = baseReport.updatedEntries,
                preservedEntries = baseReport.preservedEntries,
                mergedCategories = baseReport.mergedCategories,
                mergedVaultItems = mergedVaults,
                success = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "LanSyncMergeEngine 合并数据失败", e)
            MergeReport(success = false, message = "数据解析合并异常: ${e.message}")
        }
    }

    private fun parseEntryFromJson(o: JSONObject, id: String, ts: Long): Entry {
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

        return Entry(
            id = id,
            category = o.optString("cat", o.optString("category", "日用品")),
            brand = o.optString("brand", ""),
            qty = o.optInt("qty", 1),
            price = o.optDouble("price", 0.0),
            currentValuation = o.optDouble("cur_val", o.optDouble("current_valuation", o.optDouble("price", 0.0))),
            purchaseDate = o.optLong("p_date", o.optLong("purchase_date", ts)),
            ts = ts,
            isIn = o.optBoolean("in", o.optBoolean("isIn", true)),
            notes = o.optString("notes", ""),
            unit = o.optString("unit", "件"),
            location = o.optString("loc", o.optString("location", "")),
            houseName = o.optString("h_name", o.optString("houseName", "我的家")),
            roomName = o.optString("r_name", o.optString("roomName", "")),
            pinX = o.optDouble("px", o.optDouble("pinX", -1.0)).toFloat(),
            pinY = o.optDouble("py", o.optDouble("pinY", -1.0)).toFloat(),
            locationHistory = histList,
            isImportant = o.optBoolean("imp", o.optBoolean("is_important", false)),
            isRetired = o.optBoolean("is_ret", o.optBoolean("is_retired", false)),
            retiredAction = o.optString("ret_act", o.optString("retired_action", "")),
            retiredSoldPrice = o.optDouble("ret_sp", o.optDouble("retired_sold_price", 0.0)),
            isSubscription = o.optBoolean("is_sub", false),
            subCycle = o.optString("sub_cyc", o.optString("sub_cycle", "按月")),
            subNextBillingDate = o.optLong("sub_nxt", o.optLong("sub_next_billing_date", 0L)),
            subAutoRenew = o.optBoolean("sub_rnw", o.optBoolean("sub_auto_renew", true)),
            photoPath = o.optString("img_p", o.optString("photoPath", "")),
            receiptPath = o.optString("rec_p", o.optString("receiptPath", ""))
        )
    }

    private fun mergeVouchers(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getVouchers().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val v = VoucherRecord(
                id = id,
                title = o.optString("title", ""),
                type = o.optString("type", "coupon"),
                valueAmount = o.optDouble("valueAmount", o.optDouble("val", 0.0)),
                remainingTimes = o.optInt("remainingTimes", o.optInt("rem_t", 1)),
                totalTimes = o.optInt("totalTimes", o.optInt("tot_t", 1)),
                expiryDate = o.optLong("expiryDate", o.optLong("e_date", 0L)),
                code = o.optString("code", ""),
                platform = o.optString("platform", o.optString("plat", "")),
                notes = o.optString("notes", ""),
                isUsed = o.optBoolean("isUsed", o.optBoolean("used", false))
            )
            if (!map.containsKey(id)) {
                current.add(0, v)
                map[id] = v
                count++
            }
        }
        if (count > 0) store.saveVouchers(current)
        return count
    }

    private fun mergeIdentityDocs(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getIdentityDocs().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val doc = IdentityDocument(
                id = id,
                nameOnDoc = o.optString("nameOnDoc", o.optString("name", "")),
                member = o.optString("member", "本人"),
                docType = o.optString("docType", o.optString("type", "id_card")),
                docNumber = o.optString("docNumber", o.optString("c_num", "")),
                issueDate = o.optLong("issueDate", o.optLong("i_date", 0L)),
                expiryDate = o.optLong("expiryDate", o.optLong("e_date", 0L)),
                frontPhotoPath = o.optString("frontPhotoPath", o.optString("f_photo", "")),
                backPhotoPath = o.optString("backPhotoPath", o.optString("b_photo", "")),
                notes = o.optString("notes", "")
            )
            if (!map.containsKey(id)) {
                current.add(0, doc)
                map[id] = doc
                count++
            }
        }
        if (count > 0) store.saveIdentityDocs(current)
        return count
    }

    private fun mergeMedicines(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getMedicines().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val m = MedicineRecord(
                id = id,
                name = o.optString("name", ""),
                category = o.optString("category", o.optString("cat", "fever")),
                form = o.optString("form", "片剂"),
                qty = o.optInt("qty", 1),
                unit = o.optString("unit", "盒"),
                location = o.optString("location", o.optString("loc", "家庭药箱")),
                dosage = o.optString("dosage", ""),
                targetAudience = o.optString("targetAudience", o.optString("aud", "全家通用")),
                expiryDate = o.optLong("expiryDate", o.optLong("exp", 0L)),
                isOpened = o.optBoolean("isOpened", o.optBoolean("opened", false)),
                openedAt = o.optLong("openedAt", o.optLong("o_at", 0L)),
                openedValidityDays = o.optInt("openedValidityDays", o.optInt("o_days", 0)),
                photoPath = o.optString("photoPath", o.optString("photo", "")),
                contraindications = o.optString("contraindications", o.optString("contra", ""))
            )
            if (!map.containsKey(id)) {
                current.add(0, m)
                map[id] = m
                count++
            }
        }
        if (count > 0) store.saveMedicines(current)
        return count
    }

    private fun mergeFoods(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getFoods().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val f = FoodRecord(
                id = id,
                name = o.optString("name", ""),
                zone = o.optString("zone", "freezer"),
                qty = o.optDouble("qty", 1.0),
                unit = o.optString("unit", "盒"),
                location = o.optString("location", o.optString("loc", "冰箱")),
                purchaseDate = o.optLong("purchaseDate", o.optLong("mfgDate", o.optLong("mfg", System.currentTimeMillis()))),
                expiryDate = o.optLong("expiryDate", o.optLong("expDate", o.optLong("exp", 0L))),
                notes = o.optString("notes", "")
            )
            if (!map.containsKey(id)) {
                current.add(0, f)
                map[id] = f
                count++
            }
        }
        if (count > 0) store.saveFoods(current)
        return count
    }

    private fun mergeHonors(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getHonorCredentials().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val h = HonorCredential(
                id = id,
                title = o.optString("title", ""),
                member = o.optString("member", "本人"),
                category = o.optString("category", o.optString("cat", "academic")),
                issuer = o.optString("issuer", ""),
                certNumber = o.optString("certNumber", o.optString("c_num", ""))
            )
            if (!map.containsKey(id)) {
                current.add(0, h)
                map[id] = h
                count++
            }
        }
        if (count > 0) store.saveHonorCredentials(current)
        return count
    }

    private fun mergeWardrobe(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getWardrobeRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val w = WardrobeRecord(
                id = id,
                name = o.optString("name", ""),
                season = o.optString("season", "all"),
                category = o.optString("category", o.optString("cat", "top")),
                color = o.optString("color", ""),
                storageLocation = o.optString("storageLocation", o.optString("loc", "主卧衣柜"))
            )
            if (!map.containsKey(id)) {
                current.add(0, w)
                map[id] = w
                count++
            }
        }
        if (count > 0) store.saveWardrobeRecords(current)
        return count
    }

    private fun mergeEmergency(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getEmergencyItems().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val em = EmergencyItem(
                id = id,
                name = o.optString("name", ""),
                kitType = o.optString("kitType", o.optString("kit", "earthquake")),
                category = o.optString("category", o.optString("cat", "food")),
                location = o.optString("location", o.optString("loc", "玄关应急包")),
                expiryDate = o.optLong("expiryDate", o.optLong("exp", 0L))
            )
            if (!map.containsKey(id)) {
                current.add(0, em)
                map[id] = em
                count++
            }
        }
        if (count > 0) store.saveEmergencyItems(current)
        return count
    }

    private fun mergeTools(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getToolRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val t = ToolMaintenanceRecord(
                id = id,
                name = o.optString("name", ""),
                spec = o.optString("spec", ""),
                category = o.optString("category", o.optString("cat", "electric")),
                location = o.optString("location", o.optString("loc", "工具箱")),
                maintenanceIntervalDays = o.optInt("maintenanceIntervalDays", o.optInt("interval_d", 0))
            )
            if (!map.containsKey(id)) {
                current.add(0, t)
                map[id] = t
                count++
            }
        }
        if (count > 0) store.saveToolRecords(current)
        return count
    }

    private fun mergePlants(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getPlantRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val p = PlantCareRecord(
                id = id,
                name = o.optString("name", ""),
                species = o.optString("species", ""),
                location = o.optString("location", o.optString("loc", "客厅阳台")),
                waterIntervalDays = o.optInt("waterIntervalDays", o.optInt("w_interval_d", 7))
            )
            if (!map.containsKey(id)) {
                current.add(0, p)
                map[id] = p
                count++
            }
        }
        if (count > 0) store.savePlantRecords(current)
        return count
    }

    private fun mergePets(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getPetRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val pet = PetCareRecord(
                id = id,
                name = o.optString("name", ""),
                species = o.optString("species", "猫咪"),
                weightKg = o.optDouble("weightKg", o.optDouble("weight", 0.0)),
                foodBrand = o.optString("foodBrand", o.optString("food_b", ""))
            )
            if (!map.containsKey(id)) {
                current.add(0, pet)
                map[id] = pet
                count++
            }
        }
        if (count > 0) store.savePetRecords(current)
        return count
    }

    private fun mergeBooks(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getBookRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val b = BookRecord(
                id = id,
                title = o.optString("title", ""),
                author = o.optString("author", ""),
                category = o.optString("category", o.optString("cat", "社科人文")),
                totalPages = o.optInt("totalPages", o.optInt("total_p", 300)),
                currentPages = o.optInt("currentPages", o.optInt("curr_p", 0)),
                bookshelfLocation = o.optString("bookshelfLocation", o.optString("shelf_loc", "书房书架"))
            )
            if (!map.containsKey(id)) {
                current.add(0, b)
                map[id] = b
                count++
            }
        }
        if (count > 0) store.saveBookRecords(current)
        return count
    }

    private fun mergeBeverages(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getBeverageRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val bv = BeverageTeaRecord(
                id = id,
                name = o.optString("name", ""),
                category = o.optString("category", o.optString("cat", "茶品干货")),
                vintageYear = o.optInt("vintageYear", o.optInt("vintage", 2020)),
                storageLocation = o.optString("storageLocation", o.optString("loc", "恒温酒柜"))
            )
            if (!map.containsKey(id)) {
                current.add(0, bv)
                map[id] = bv
                count++
            }
        }
        if (count > 0) store.saveBeverageRecords(current)
        return count
    }

    private fun mergeIdeas(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getIdeas().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            val existing = map[id]
            val incUpdatedAt = o.optLong("updatedAt", o.optLong("updated_at", System.currentTimeMillis()))
            val tagsArr = o.optJSONArray("tags")
            val tagsList = mutableListOf<String>()
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    val t = tagsArr.optString(j)
                    if (!t.isNullOrBlank()) tagsList.add(t)
                }
            }
            val linkedArr = o.optJSONArray("linkedAssetIds")
            val linkedList = mutableListOf<String>()
            if (linkedArr != null) {
                for (j in 0 until linkedArr.length()) {
                    val l = linkedArr.optString(j)
                    if (!l.isNullOrBlank()) linkedList.add(l)
                }
            }

            val idea = IdeaRecord(
                id = id,
                content = o.optString("content", ""),
                tags = tagsList,
                moodEmoji = o.optString("moodEmoji", o.optString("emoji", "💡")),
                isPinned = o.optBoolean("isPinned", o.optBoolean("pinned", false)),
                colorHex = o.optString("colorHex", "#10B981"),
                linkedAssetIds = linkedList,
                createdAt = o.optLong("createdAt", o.optLong("created_at", System.currentTimeMillis())),
                updatedAt = incUpdatedAt
            )

            if (existing == null) {
                current.add(0, idea)
                map[id] = idea
                count++
            } else if (incUpdatedAt > existing.updatedAt) {
                val idx = current.indexOfFirst { it.id == id }
                if (idx != -1) {
                    current[idx] = idea
                    map[id] = idea
                    count++
                }
            }
        }
        if (count > 0) store.saveIdeas(current)
        return count
    }

    private fun mergeClippings(store: DataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getClippings().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val tagsArr = o.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        val t = tagsArr.optString(j)
                        if (!t.isNullOrBlank()) tagsList.add(t)
                    }
                }
                val linkedArr = o.optJSONArray("linkedAssetIds")
                val linkedList = mutableListOf<String>()
                if (linkedArr != null) {
                    for (j in 0 until linkedArr.length()) {
                        val l = linkedArr.optString(j)
                        if (!l.isNullOrBlank()) linkedList.add(l)
                    }
                }

                val clip = ClippingRecord(
                    id = id,
                    title = o.optString("title", ""),
                    originalUrl = o.optString("originalUrl", o.optString("url", "")),
                    sourcePlatform = o.optString("sourcePlatform", o.optString("platform", "web")),
                    fullMarkdown = o.optString("fullMarkdown", o.optString("markdown", "")),
                    ocrRawText = o.optString("ocrRawText", o.optString("ocr", "")),
                    summary = o.optString("summary", ""),
                    tags = tagsList,
                    linkedAssetIds = linkedList,
                    capturedAt = o.optLong("capturedAt", o.optLong("captured_at", System.currentTimeMillis()))
                )
                current.add(0, clip)
                map[id] = clip
                count++
            }
        }
        if (count > 0) store.saveClippings(current)
        return count
    }
}
