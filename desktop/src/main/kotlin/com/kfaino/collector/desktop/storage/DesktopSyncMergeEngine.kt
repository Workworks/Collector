package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DesktopMergeReport(
    val insertedEntries: Int = 0,
    val updatedEntries: Int = 0,
    val preservedEntries: Int = 0,
    val mergedCategories: Int = 0,
    val mergedVaultItems: Int = 0,
    val success: Boolean = true,
    val message: String = "合并成功"
) {
    fun summary(): String {
        return "桌面端增量对撞合并完成：新增资产 ${insertedEntries} 件，更新资产 ${updatedEntries} 件，保留 ${preservedEntries} 件，合并分类 ${mergedCategories} 个，合并 12 馆物资 ${mergedVaultItems} 项。"
    }
}

/**
 * 桌面端 P2P 增量对撞合并核心引擎
 */
object DesktopSyncMergeEngine {

    fun merge(store: DesktopDataStore, incomingJsonStr: String): DesktopMergeReport {
        if (incomingJsonStr.isBlank()) {
            return DesktopMergeReport(success = false, message = "传入数据为空")
        }

        return try {
            val root = JSONObject(incomingJsonStr)
            var inserted = 0
            var updated = 0
            var preserved = 0
            var mergedCats = 0
            var mergedVaults = 0

            // 1. 分类合并
            val incCatArr = root.optJSONArray("categories")
            if (incCatArr != null) {
                val currentCats = store.getCategories().toMutableList()
                val initialCatCount = currentCats.size
                for (i in 0 until incCatArr.length()) {
                    val cat = incCatArr.optString(i).trim()
                    if (cat.isNotEmpty() && !currentCats.contains(cat)) {
                        currentCats.add(cat)
                    }
                }
                if (currentCats.size > initialCatCount) {
                    store.setCategories(currentCats)
                    mergedCats = currentCats.size - initialCatCount
                }
            }

            // 2. 主资产合并
            val incEntryArr = root.optJSONArray("entries")
            if (incEntryArr != null) {
                val currentEntries = store.loadAll().toMutableList()
                val entryMap = currentEntries.associateBy { it.id }.toMutableMap()

                for (i in 0 until incEntryArr.length()) {
                    val o = incEntryArr.getJSONObject(i)
                    val incomingId = o.optString("id", UUID.randomUUID().toString())
                    val incomingTs = o.optLong("ts", System.currentTimeMillis())

                    val incomingEntry = Entry(
                        id = incomingId,
                        brand = o.optString("brand", ""),
                        category = o.optString("cat", o.optString("category", "日用品")),
                        price = o.optDouble("price", 0.0),
                        qty = o.optInt("qty", 1),
                        unit = o.optString("unit", "件"),
                        location = o.optString("loc", o.optString("location", "")),
                        ts = incomingTs,
                        isIn = o.optBoolean("in", o.optBoolean("isIn", true)),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("img_p", o.optString("photoPath", ""))
                    )

                    val existing = entryMap[incomingId]
                    if (existing == null) {
                        currentEntries.add(0, incomingEntry)
                        entryMap[incomingId] = incomingEntry
                        inserted++
                    } else {
                        if (incomingEntry.ts > existing.ts) {
                            val idx = currentEntries.indexOfFirst { it.id == incomingId }
                            if (idx != -1) {
                                currentEntries[idx] = incomingEntry
                                entryMap[incomingId] = incomingEntry
                                updated++
                            }
                        } else {
                            preserved++
                        }
                    }
                }
                store.saveAll(currentEntries)
            }

            // 3. 12 馆合并
            mergedVaults += mergeVouchers(store, root.optJSONArray("vouchers"))
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

            DesktopMergeReport(
                insertedEntries = inserted,
                updatedEntries = updated,
                preservedEntries = preserved,
                mergedCategories = mergedCats,
                mergedVaultItems = mergedVaults,
                success = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DesktopMergeReport(success = false, message = "数据对撞合并异常: ${e.message}")
        }
    }

    private fun mergeVouchers(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getVouchers().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val v = VoucherRecord(
                    id = id,
                    title = o.optString("title", ""),
                    valueAmount = o.optDouble("val", o.optDouble("valueAmount", 0.0)),
                    expiryDate = o.optLong("e_date", o.optLong("expiryDate", 0L))
                )
                current.add(0, v)
                map[id] = v
                count++
            }
        }
        if (count > 0) store.saveVouchers(current)
        return count
    }

    private fun mergeMedicines(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getMedicines().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val m = MedicineRecord(
                    id = id,
                    name = o.optString("name", ""),
                    expiryDate = o.optLong("exp", o.optLong("expiryDate", 0L))
                )
                current.add(0, m)
                map[id] = m
                count++
            }
        }
        if (count > 0) store.saveMedicines(current)
        return count
    }

    private fun mergeFoods(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getFoods().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val f = FoodRecord(
                    id = id,
                    name = o.optString("name", ""),
                    expDate = o.optLong("exp", o.optLong("expDate", 0L))
                )
                current.add(0, f)
                map[id] = f
                count++
            }
        }
        if (count > 0) store.saveFoods(current)
        return count
    }

    private fun mergeHonors(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getHonorCredentials().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val h = HonorCredentialRecord(
                    id = id,
                    title = o.optString("title", ""),
                    issuer = o.optString("issuer", "")
                )
                current.add(0, h)
                map[id] = h
                count++
            }
        }
        if (count > 0) store.saveHonorCredentials(current)
        return count
    }

    private fun mergeWardrobe(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getWardrobeRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val w = WardrobeRecord(
                    id = id,
                    name = o.optString("name", ""),
                    season = o.optString("season", "all")
                )
                current.add(0, w)
                map[id] = w
                count++
            }
        }
        if (count > 0) store.saveWardrobeRecords(current)
        return count
    }

    private fun mergeEmergency(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getEmergencyItems().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val em = EmergencyItem(
                    id = id,
                    name = o.optString("name", ""),
                    kitType = o.optString("kit", o.optString("kitType", "earthquake"))
                )
                current.add(0, em)
                map[id] = em
                count++
            }
        }
        if (count > 0) store.saveEmergencyItems(current)
        return count
    }

    private fun mergeTools(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getToolRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val t = ToolMaintenanceRecord(
                    id = id,
                    name = o.optString("name", ""),
                    spec = o.optString("spec", "")
                )
                current.add(0, t)
                map[id] = t
                count++
            }
        }
        if (count > 0) store.saveToolRecords(current)
        return count
    }

    private fun mergePlants(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getPlantRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val p = PlantCareRecord(
                    id = id,
                    name = o.optString("name", ""),
                    species = o.optString("species", "")
                )
                current.add(0, p)
                map[id] = p
                count++
            }
        }
        if (count > 0) store.savePlantRecords(current)
        return count
    }

    private fun mergePets(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getPetRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val pet = PetCareRecord(
                    id = id,
                    name = o.optString("name", ""),
                    species = o.optString("species", "cat")
                )
                current.add(0, pet)
                map[id] = pet
                count++
            }
        }
        if (count > 0) store.savePetRecords(current)
        return count
    }

    private fun mergeBooks(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getBookRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val b = BookRecord(
                    id = id,
                    title = o.optString("title", ""),
                    author = o.optString("author", "")
                )
                current.add(0, b)
                map[id] = b
                count++
            }
        }
        if (count > 0) store.saveBookRecords(current)
        return count
    }

    private fun mergeBeverages(store: DesktopDataStore, arr: JSONArray?): Int {
        if (arr == null) return 0
        val current = store.getBeverageTeaRecords().toMutableList()
        val map = current.associateBy { it.id }.toMutableMap()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", UUID.randomUUID().toString())
            if (!map.containsKey(id)) {
                val bv = BeverageTeaRecord(
                    id = id,
                    name = o.optString("name", ""),
                    vintageYear = o.optInt("vintage", o.optInt("vintageYear", 0))
                )
                current.add(0, bv)
                map[id] = bv
                count++
            }
        }
        if (count > 0) store.saveBeverageTeaRecords(current)
        return count
    }

    private fun mergeIdeas(store: DesktopDataStore, arr: JSONArray?): Int {
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

    private fun mergeClippings(store: DesktopDataStore, arr: JSONArray?): Int {
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
