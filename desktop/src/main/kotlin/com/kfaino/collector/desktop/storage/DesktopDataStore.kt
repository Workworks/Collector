package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import com.kfaino.collecter.core.BackupDocument
import com.kfaino.collecter.core.WireAliases
import com.kfaino.collecter.core.SnapshotSync

/**
 * 跨平台桌面端高可靠本地持久化存储
 *
 * 遵循 capital-agent-system 规范：
 * - 默认数据目录由 [DesktopDataDirectory.resolve] 确定（%LOCALAPPDATA%\CollecterStandalone\data）
 * - 100% 涵盖主资产库与 12 大第一性原理专业收纳馆
 * - 提供完整的 JSON 序列化与反序列化，与 Android 端无缝互通
 */
class DesktopDataStore(customDataDir: File? = null) {

    val dataDir: File = customDataDir ?: DesktopDataDirectory.resolve()

    private val dataFile = File(dataDir, "collector_data.json")
    private val configFile = File(dataDir, "config.json")
    private var rawSnapshot = JSONObject()

    private var inMemoryEntries = mutableListOf<Entry>()
    private var inMemoryCategories = mutableListOf<String>()
    private var inMemoryHouses = mutableListOf<House>()

    // 12 大第一性原理收纳馆内存缓存
    private var inMemoryVouchers = mutableListOf<VoucherRecord>()
    private var inMemoryIdentityDocs = mutableListOf<IdentityDocRecord>()
    private var inMemoryMedicines = mutableListOf<MedicineRecord>()
    private var inMemoryFoods = mutableListOf<FoodRecord>()
    private var inMemoryHonors = mutableListOf<HonorCredentialRecord>()
    private var inMemoryWardrobe = mutableListOf<WardrobeRecord>()
    private var inMemoryEmergency = mutableListOf<EmergencyItem>()
    private var inMemoryTools = mutableListOf<ToolMaintenanceRecord>()
    private var inMemoryPlants = mutableListOf<PlantCareRecord>()
    private var inMemoryPets = mutableListOf<PetCareRecord>()
    private var inMemoryBooks = mutableListOf<BookRecord>()
    private var inMemoryBeverages = mutableListOf<BeverageTeaRecord>()
    private var inMemoryIdeas = mutableListOf<IdeaRecord>()
    private var inMemoryClippings = mutableListOf<ClippingRecord>()

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
            val root = WireAliases.convert(JSONObject(content), expand = true)
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

                val cycleStr = o.optString("sub_cycle", o.optString("sub_cyc", "MONTHLY"))
                val cycle = try { SubCycle.valueOf(cycleStr) } catch (_: Exception) { SubCycle.MONTHLY }

                list.add(
                    Entry(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        brand = o.optString("brand", ""),
                        category = o.optString("category", o.optString("cat", "日用品")),
                        price = o.optDouble("price", 0.0),
                        qty = o.optInt("qty", 1),
                        unit = o.optString("unit", "件"),
                        location = o.optString("location", o.optString("loc", "")),
                        houseName = o.optString("houseName", o.optString("h_name", "我的家")),
                        roomName = o.optString("roomName", o.optString("r_name", "")),
                        pinX = o.optDouble("pinX", o.optDouble("px", -1.0)).toFloat(),
                        pinY = o.optDouble("pinY", o.optDouble("py", -1.0)).toFloat(),
                        locationHistory = histList,
                        isIn = o.optBoolean("isIn", o.optBoolean("in", true)),
                        ts = o.optLong("ts", System.currentTimeMillis()),
                        notes = o.optString("notes", ""),
                        photoPath = o.optString("photoPath", o.optString("img_p", "")),
                        receiptPath = o.optString("receiptPath", o.optString("rec_p", "")),
                        barcode = o.optString("barcode", ""),
                        isDepreciating = o.optBoolean("is_depreciating", true),
                        mfgDate = o.optLong("mfg_date", o.optLong("m_date", 0L)),
                        expDate = o.optLong("exp_date", o.optLong("e_date", 0L)),
                        isDurable = o.optBoolean("is_durable", false),
                        durableStartDate = o.optLong("durable_start_date", 0L),
                        isConsumable = o.optBoolean("is_consumable", false),
                        originalPrice = o.optDouble("original_price", o.optDouble("price", 0.0)),
                        purchaseDate = o.optLong("purchase_date", o.optLong("p_date", o.optLong("ts", System.currentTimeMillis()))),
                        currentValuation = o.optDouble("current_valuation", o.optDouble("cur_val", o.optDouble("price", 0.0))),
                        lastValuationDate = o.optLong("last_valuation_date", o.optLong("ts", System.currentTimeMillis())),
                        targetResidualRate = o.optDouble("target_residual_rate", 0.1),
                        expectedLifeYears = o.optDouble("expected_life_years", 3.0),
                        isRetired = o.optBoolean("is_retired", o.optBoolean("is_ret", false)),
                        retiredDate = o.optLong("retired_date", o.optLong("ret_at", 0L)),
                        retiredAction = o.optString("retired_action", o.optString("ret_act", "")),
                        retiredSoldPrice = o.optDouble("retired_sold_price", o.optDouble("ret_sp", 0.0)),
                        retiredNote = o.optString("retired_note", o.optString("ret_note", "")),
                        isSubscription = o.optBoolean("is_sub", false),
                        subPrice = o.optDouble("sub_price", o.optDouble("price", 0.0)),
                        subCycle = cycle,
                        subStartDate = o.optLong("sub_start_date", o.optLong("ts", System.currentTimeMillis())),
                        subNextBillingDate = o.optLong("sub_next_billing_date", o.optLong("sub_nxt", o.optLong("ts", System.currentTimeMillis()) + 30L * 24 * 60 * 60 * 1000)),
                        subAutoRenew = o.optBoolean("sub_auto_renew", o.optBoolean("sub_rnw", true)),
                        isImportant = o.optBoolean("is_important", o.optBoolean("imp", false)),
                        reminderEnabled = o.optBoolean("reminder_enabled", o.optBoolean("rem_en", false)),
                        reminderIntervalDays = o.optInt("reminder_interval_days", o.optInt("rem_int", 1)),
                        lastCheckedDate = o.optLong("last_checked_date", o.optLong("chk_ts", o.optLong("ts", System.currentTimeMillis())))
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

            // 01. 卡券
            inMemoryVouchers = parseList(root.optJSONArray("vouchers")) { vo ->
                VoucherRecord(
                    id = vo.optString("id", UUID.randomUUID().toString()),
                    title = vo.optString("title", ""),
                    type = vo.optString("type", "coupon"),
                    valueAmount = vo.optDouble("valueAmount", vo.optDouble("val", 0.0)),
                    minSpend = vo.optDouble("minSpend", vo.optDouble("min", 0.0)),
                    remainingTimes = vo.optInt("remainingTimes", vo.optInt("rem_t", 1)),
                    totalTimes = vo.optInt("totalTimes", vo.optInt("tot_t", 1)),
                    startDate = vo.optLong("startDate", vo.optLong("s_date", System.currentTimeMillis())),
                    expiryDate = vo.optLong("expiryDate", vo.optLong("e_date", 0L)),
                    code = vo.optString("code", ""),
                    platform = vo.optString("platform", vo.optString("plat", "")),
                    photoPath = vo.optString("photoPath", vo.optString("photo", "")),
                    notes = vo.optString("notes", ""),
                    isUsed = vo.optBoolean("isUsed", vo.optBoolean("used", false)),
                    usedAt = vo.optLong("usedAt", vo.optLong("used_at", 0L))
                )
            }

            // 02. 证照
            inMemoryIdentityDocs = parseList(root.optJSONArray("identity_docs")) { io ->
                IdentityDocRecord(
                    id = io.optString("id", UUID.randomUUID().toString()),
                    nameOnDoc = io.optString("nameOnDoc", io.optString("name", "")),
                    member = io.optString("member", "本人"),
                    docType = io.optString("docType", io.optString("type", "id_card")),
                    certNumber = io.optString("certNumber", io.optString("c_num", "")),
                    issueDate = io.optLong("issueDate", io.optLong("i_date", 0L)),
                    expiryDate = io.optLong("expiryDate", io.optLong("e_date", 0L)),
                    frontPhotoPath = io.optString("frontPhotoPath", io.optString("f_photo", "")),
                    backPhotoPath = io.optString("backPhotoPath", io.optString("b_photo", "")),
                    notes = io.optString("notes", ""),
                    hasAnnualAudit = io.optBoolean("hasAnnualAudit", io.optBoolean("audit", false))
                )
            }

            // 03. 药箱
            inMemoryMedicines = parseList(root.optJSONArray("medicines")) { mo ->
                MedicineRecord(
                    id = mo.optString("id", UUID.randomUUID().toString()),
                    name = mo.optString("name", ""),
                    category = mo.optString("category", mo.optString("cat", "fever")),
                    form = mo.optString("form", "片剂"),
                    qty = mo.optInt("qty", 1),
                    unit = mo.optString("unit", "盒"),
                    location = mo.optString("location", mo.optString("loc", "家庭药箱")),
                    dosage = mo.optString("dosage", ""),
                    targetAudience = mo.optString("targetAudience", mo.optString("aud", "全家通用")),
                    expiryDate = mo.optLong("expiryDate", mo.optLong("exp", 0L)),
                    isOpened = mo.optBoolean("isOpened", mo.optBoolean("opened", false)),
                    openedAt = mo.optLong("openedAt", mo.optLong("o_at", 0L)),
                    openedValidityDays = mo.optInt("openedValidityDays", mo.optInt("o_days", 0)),
                    photoPath = mo.optString("photoPath", mo.optString("photo", "")),
                    contraindications = mo.optString("contraindications", mo.optString("contra", ""))
                )
            }

            // 04. 食材
            inMemoryFoods = parseList(root.optJSONArray("foods")) { fo ->
                FoodRecord(
                    id = fo.optString("id", UUID.randomUUID().toString()),
                    name = fo.optString("name", ""),
                    zone = fo.optString("zone", "freezer"),
                    qty = fo.optInt("qty", 1),
                    unit = fo.optString("unit", "盒"),
                    location = fo.optString("location", fo.optString("loc", "冰箱")),
                    mfgDate = fo.optLong("mfgDate", fo.optLong("mfg", 0L)),
                    expDate = fo.optLong("expDate", fo.optLong("exp", 0L)),
                    isOpened = fo.optBoolean("isOpened", fo.optBoolean("opened", false)),
                    openedAt = fo.optLong("openedAt", fo.optLong("o_at", 0L)),
                    photoPath = fo.optString("photoPath", fo.optString("photo", "")),
                    notes = fo.optString("notes", ""),
                    storageMethod = fo.optString("storageMethod", fo.optString("method", "常规冷藏")),
                    consumeTargetDate = fo.optLong("consumeTargetDate", fo.optLong("consume_t", 0L))
                )
            }

            // 05. 荣誉
            inMemoryHonors = parseList(root.optJSONArray("honors")) { ho ->
                HonorCredentialRecord(
                    id = ho.optString("id", UUID.randomUUID().toString()),
                    title = ho.optString("title", ""),
                    member = ho.optString("member", "本人"),
                    category = ho.optString("category", ho.optString("cat", "academic")),
                    issuer = ho.optString("issuer", ""),
                    certNumber = ho.optString("certNumber", ho.optString("c_num", "")),
                    certDate = ho.optLong("certDate", ho.optLong("c_date", 0L)),
                    hasAnnualAudit = ho.optBoolean("hasAnnualAudit", ho.optBoolean("has_audit", false)),
                    nextAuditDate = ho.optLong("nextAuditDate", ho.optLong("audit_date", 0L)),
                    photoPath = ho.optString("photoPath", ho.optString("photo", "")),
                    notes = ho.optString("notes", "")
                )
            }

            // 06. 衣橱
            inMemoryWardrobe = parseList(root.optJSONArray("wardrobe")) { wo ->
                WardrobeRecord(
                    id = wo.optString("id", UUID.randomUUID().toString()),
                    name = wo.optString("name", ""),
                    season = wo.optString("season", "all"),
                    category = wo.optString("category", wo.optString("cat", "top")),
                    color = wo.optString("color", ""),
                    material = wo.optString("material", ""),
                    storageLocation = wo.optString("storageLocation", wo.optString("loc", "主卧衣柜")),
                    price = wo.optDouble("price", 0.0),
                    photoPath = wo.optString("photoPath", wo.optString("photo", "")),
                    wearCount = wo.optInt("wearCount", wo.optInt("wear_c", 0)),
                    lastWornAt = wo.optLong("lastWornAt", wo.optLong("last_w", 0L)),
                    isSealed = wo.optBoolean("isSealed", wo.optBoolean("sealed", false)),
                    sealedAt = wo.optLong("sealedAt", wo.optLong("sealed_at", 0L)),
                    notes = wo.optString("notes", "")
                )
            }

            // 07. 应急
            inMemoryEmergency = parseList(root.optJSONArray("emergency")) { eo ->
                EmergencyItem(
                    id = eo.optString("id", UUID.randomUUID().toString()),
                    name = eo.optString("name", ""),
                    kitType = eo.optString("kitType", eo.optString("kit", "earthquake")),
                    category = eo.optString("category", eo.optString("cat", "food")),
                    qty = eo.optInt("qty", 1),
                    unit = eo.optString("unit", "件"),
                    location = eo.optString("location", eo.optString("loc", "玄关应急包")),
                    expiryDate = eo.optLong("expiryDate", eo.optLong("exp", 0L)),
                    lastTestedAt = eo.optLong("lastTestedAt", eo.optLong("checked_at", 0L)),
                    photoPath = eo.optString("photoPath", eo.optString("photo", "")),
                    notes = eo.optString("notes", ""),
                    importanceLevel = eo.optString("importanceLevel", eo.optString("importance", "must_have"))
                )
            }

            // 08. 工具
            inMemoryTools = parseList(root.optJSONArray("tools")) { to ->
                ToolMaintenanceRecord(
                    id = to.optString("id", UUID.randomUUID().toString()),
                    name = to.optString("name", ""),
                    spec = to.optString("spec", ""),
                    category = to.optString("category", to.optString("cat", "electric")),
                    qty = to.optInt("qty", 1),
                    unit = to.optString("unit", "件"),
                    location = to.optString("location", to.optString("loc", "工具箱")),
                    maintenanceIntervalDays = to.optInt("maintenanceIntervalDays", to.optInt("interval_d", 0)),
                    lastMaintainedAt = to.optLong("lastMaintainedAt", to.optLong("last_m", 0L)),
                    photoPath = to.optString("photoPath", to.optString("photo", "")),
                    notes = to.optString("notes", "")
                )
            }

            // 09. 绿植
            inMemoryPlants = parseList(root.optJSONArray("plants")) { po ->
                PlantCareRecord(
                    id = po.optString("id", UUID.randomUUID().toString()),
                    name = po.optString("name", ""),
                    species = po.optString("species", ""),
                    lightDemand = po.optString("lightDemand", po.optString("light", "scattered")),
                    location = po.optString("location", po.optString("loc", "客厅阳台")),
                    waterIntervalDays = po.optInt("waterIntervalDays", po.optInt("w_interval_d", 7)),
                    lastWateredAt = po.optLong("lastWateredAt", po.optLong("last_w", 0L)),
                    fertilizeIntervalDays = po.optInt("fertilizeIntervalDays", po.optInt("f_interval_d", 30)),
                    lastFertilizedAt = po.optLong("lastFertilizedAt", po.optLong("last_f", 0L)),
                    photoPath = po.optString("photoPath", po.optString("photo", "")),
                    careTips = po.optString("careTips", po.optString("tips", "")),
                    plantedAt = po.optLong("plantedAt", po.optLong("planted_at", System.currentTimeMillis()))
                )
            }

            // 10. 宠物
            inMemoryPets = parseList(root.optJSONArray("pets")) { peto ->
                PetCareRecord(
                    id = peto.optString("id", UUID.randomUUID().toString()),
                    name = peto.optString("name", ""),
                    species = peto.optString("species", "cat"),
                    breed = peto.optString("breed", ""),
                    weightKg = peto.optDouble("weightKg", peto.optDouble("weight", 0.0)),
                    chipNumber = peto.optString("chipNumber", peto.optString("chip_no", "")),
                    foodBrand = peto.optString("foodBrand", peto.optString("food_b", "")),
                    foodStorageLocation = peto.optString("foodStorageLocation", peto.optString("food_loc", "")),
                    dewormIntervalDays = peto.optInt("dewormIntervalDays", peto.optInt("deworm_d", 30)),
                    lastDewormedAt = peto.optLong("lastDewormedAt", peto.optLong("last_deworm", 0L)),
                    vaccineIntervalDays = peto.optInt("vaccineIntervalDays", peto.optInt("vaccine_d", 365)),
                    lastVaccinatedAt = peto.optLong("lastVaccinatedAt", peto.optLong("last_vaccine", 0L)),
                    photoPath = peto.optString("photoPath", peto.optString("photo", "")),
                    notes = peto.optString("notes", "")
                )
            }

            // 11. 藏书
            inMemoryBooks = parseList(root.optJSONArray("books")) { bo ->
                BookRecord(
                    id = bo.optString("id", UUID.randomUUID().toString()),
                    title = bo.optString("title", ""),
                    author = bo.optString("author", ""),
                    category = bo.optString("category", bo.optString("cat", "general")),
                    totalPages = bo.optInt("totalPages", bo.optInt("total_p", 0)),
                    currentPages = bo.optInt("currentPages", bo.optInt("curr_p", 0)),
                    bookshelfLocation = bo.optString("bookshelfLocation", bo.optString("shelf_loc", "书房书架")),
                    rating = bo.optInt("rating", 5),
                    borrowerName = bo.optString("borrowerName", bo.optString("lent_to", "")),
                    lentDate = bo.optLong("lentDate", bo.optLong("lent_at", 0L)),
                    notes = bo.optString("notes", ""),
                    coverPhotoPath = bo.optString("coverPhotoPath", bo.optString("cover_photo", ""))
                )
            }

            // 12. 茶窖
            inMemoryBeverages = parseList(root.optJSONArray("beverages")) { bvo ->
                BeverageTeaRecord(
                    id = bvo.optString("id", UUID.randomUUID().toString()),
                    name = bvo.optString("name", ""),
                    category = bvo.optString("category", bvo.optString("cat", "liquor")),
                    vintageYear = bvo.optInt("vintageYear", bvo.optInt("vintage", 0)),
                    optimalAgingYear = bvo.optInt("optimalAgingYear", bvo.optInt("peak_y", 0)),
                    qty = bvo.optInt("qty", 1),
                    unit = bvo.optString("unit", "瓶"),
                    storageLocation = bvo.optString("storageLocation", bvo.optString("loc", "恒温酒柜")),
                    originRegion = bvo.optString("originRegion", bvo.optString("origin", "")),
                    isOpened = bvo.optBoolean("isOpened", bvo.optBoolean("opened", false)),
                    openedAt = bvo.optLong("openedAt", bvo.optLong("opened_at", 0L)),
                    openedPreserveDays = bvo.optInt("openedPreserveDays", bvo.optInt("preserve_d", 0)),
                    photoPath = bvo.optString("photoPath", bvo.optString("photo", "")),
                    notes = bvo.optString("notes", "")
                )
            }

            // 13. 💡 想法
            inMemoryIdeas = parseList(root.optJSONArray("ideas")) { ido ->
                val tagsArr = ido.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(t))
                }
                val linkedArr = ido.optJSONArray("linked_ids")
                val linkedList = mutableListOf<String>()
                if (linkedArr != null) {
                    for (l in 0 until linkedArr.length()) linkedList.add(linkedArr.optString(l))
                }
                IdeaRecord(
                    id = ido.optString("id", UUID.randomUUID().toString()),
                    content = ido.optString("content", ""),
                    tags = tagsList,
                    voiceMemoPath = ido.optString("voice", ""),
                    moodEmoji = ido.optString("emoji", "💡"),
                    isPinned = ido.optBoolean("pinned", false),
                    colorHex = ido.optString("color", "#10B981"),
                    linkedAssetIds = linkedList,
                    createdAt = ido.optLong("created_at", System.currentTimeMillis()),
                    updatedAt = ido.optLong("updated_at", System.currentTimeMillis())
                )
            }

            // 14. 📰 剪藏
            inMemoryClippings = parseList(root.optJSONArray("clippings")) { clo ->
                val tagsArr = clo.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(t))
                }
                val imgArr = clo.optJSONArray("images")
                val imgList = mutableListOf<String>()
                if (imgArr != null) {
                    for (img in 0 until imgArr.length()) imgList.add(imgArr.optString(img))
                }
                val linkedArr = clo.optJSONArray("linked_ids")
                val linkedList = mutableListOf<String>()
                if (linkedArr != null) {
                    for (l in 0 until linkedArr.length()) linkedList.add(linkedArr.optString(l))
                }
                ClippingRecord(
                    id = clo.optString("id", UUID.randomUUID().toString()),
                    title = clo.optString("title", ""),
                    originalUrl = clo.optString("url", ""),
                    sourcePlatform = clo.optString("platform", "screenshot"),
                    fullMarkdown = clo.optString("markdown", ""),
                    ocrRawText = clo.optString("ocr", ""),
                    localImagePaths = imgList,
                    summary = clo.optString("summary", ""),
                    tags = tagsList,
                    readingProgress = clo.optDouble("progress", 0.0).toFloat(),
                    isArchived = clo.optBoolean("archived", false),
                    linkedAssetIds = linkedList,
                    capturedAt = clo.optLong("captured_at", System.currentTimeMillis())
                )
            }

            inMemoryEntries = list
            rawSnapshot = WireAliases.convert(root)
            list
        } catch (e: Exception) {
            throw IllegalStateException("读取数据失败；原文件已保留，拒绝以空数据覆盖", e)
        }
    }

    private inline fun <T> parseList(arr: JSONArray?, mapper: (JSONObject) -> T): MutableList<T> {
        val list = mutableListOf<T>()
        if (arr == null) return list
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(mapper(obj))
        }
        return list
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

        // 12 馆持久化序列化
        root.put("vouchers", serializeList(inMemoryVouchers) { v ->
            JSONObject().put("id", v.id).put("title", v.title).put("type", v.type)
                .put("valueAmount", v.valueAmount).put("minSpend", v.minSpend)
                .put("remainingTimes", v.remainingTimes).put("totalTimes", v.totalTimes)
                .put("startDate", v.startDate).put("expiryDate", v.expiryDate)
                .put("code", v.code).put("platform", v.platform)
                .put("photoPath", v.photoPath).put("notes", v.notes)
                .put("isUsed", v.isUsed).put("usedAt", v.usedAt)
        })

        root.put("identity_docs", serializeList(inMemoryIdentityDocs) { i ->
            JSONObject().put("id", i.id).put("nameOnDoc", i.nameOnDoc).put("member", i.member)
                .put("docType", i.docType).put("certNumber", i.certNumber)
                .put("issueDate", i.issueDate).put("expiryDate", i.expiryDate)
                .put("frontPhotoPath", i.frontPhotoPath).put("backPhotoPath", i.backPhotoPath)
                .put("notes", i.notes).put("hasAnnualAudit", i.hasAnnualAudit)
        })

        root.put("medicines", serializeList(inMemoryMedicines) { m ->
            JSONObject().put("id", m.id).put("name", m.name).put("category", m.category)
                .put("form", m.form).put("qty", m.qty).put("unit", m.unit)
                .put("location", m.location).put("dosage", m.dosage)
                .put("targetAudience", m.targetAudience).put("expiryDate", m.expiryDate)
                .put("isOpened", m.isOpened).put("openedAt", m.openedAt)
                .put("openedValidityDays", m.openedValidityDays)
                .put("photoPath", m.photoPath).put("contraindications", m.contraindications)
        })

        root.put("foods", serializeList(inMemoryFoods) { f ->
            JSONObject().put("id", f.id).put("name", f.name).put("zone", f.zone)
                .put("qty", f.qty).put("unit", f.unit).put("location", f.location)
                .put("mfgDate", f.mfgDate).put("expDate", f.expDate)
                .put("isOpened", f.isOpened).put("openedAt", f.openedAt)
                .put("photoPath", f.photoPath).put("notes", f.notes)
                .put("storageMethod", f.storageMethod).put("consumeTargetDate", f.consumeTargetDate)
        })

        root.put("honors", serializeList(inMemoryHonors) { h ->
            JSONObject().put("id", h.id).put("title", h.title).put("member", h.member)
                .put("category", h.category).put("issuer", h.issuer)
                .put("certNumber", h.certNumber).put("certDate", h.certDate)
                .put("hasAnnualAudit", h.hasAnnualAudit).put("nextAuditDate", h.nextAuditDate)
                .put("photoPath", h.photoPath).put("notes", h.notes)
        })

        root.put("wardrobe", serializeList(inMemoryWardrobe) { w ->
            JSONObject().put("id", w.id).put("name", w.name).put("season", w.season)
                .put("category", w.category).put("color", w.color).put("material", w.material)
                .put("storageLocation", w.storageLocation).put("price", w.price)
                .put("photoPath", w.photoPath).put("wearCount", w.wearCount)
                .put("lastWornAt", w.lastWornAt).put("isSealed", w.isSealed)
                .put("sealedAt", w.sealedAt).put("notes", w.notes)
        })

        root.put("emergency", serializeList(inMemoryEmergency) { em ->
            JSONObject().put("id", em.id).put("name", em.name).put("kitType", em.kitType)
                .put("category", em.category).put("qty", em.qty).put("unit", em.unit)
                .put("location", em.location).put("expiryDate", em.expiryDate)
                .put("lastTestedAt", em.lastTestedAt).put("photoPath", em.photoPath)
                .put("notes", em.notes).put("importanceLevel", em.importanceLevel)
        })

        root.put("tools", serializeList(inMemoryTools) { t ->
            JSONObject().put("id", t.id).put("name", t.name).put("spec", t.spec)
                .put("category", t.category).put("qty", t.qty).put("unit", t.unit)
                .put("location", t.location)
                .put("maintenanceIntervalDays", t.maintenanceIntervalDays)
                .put("lastMaintainedAt", t.lastMaintainedAt).put("photoPath", t.photoPath)
                .put("notes", t.notes)
        })

        root.put("plants", serializeList(inMemoryPlants) { p ->
            JSONObject().put("id", p.id).put("name", p.name).put("species", p.species)
                .put("lightDemand", p.lightDemand).put("location", p.location)
                .put("waterIntervalDays", p.waterIntervalDays).put("lastWateredAt", p.lastWateredAt)
                .put("fertilizeIntervalDays", p.fertilizeIntervalDays)
                .put("lastFertilizedAt", p.lastFertilizedAt).put("photoPath", p.photoPath)
                .put("careTips", p.careTips).put("plantedAt", p.plantedAt)
        })

        root.put("pets", serializeList(inMemoryPets) { pet ->
            JSONObject().put("id", pet.id).put("name", pet.name).put("species", pet.species)
                .put("breed", pet.breed).put("weightKg", pet.weightKg)
                .put("chipNumber", pet.chipNumber).put("foodBrand", pet.foodBrand)
                .put("foodStorageLocation", pet.foodStorageLocation)
                .put("dewormIntervalDays", pet.dewormIntervalDays)
                .put("lastDewormedAt", pet.lastDewormedAt)
                .put("vaccineIntervalDays", pet.vaccineIntervalDays)
                .put("lastVaccinatedAt", pet.lastVaccinatedAt).put("photoPath", pet.photoPath)
                .put("notes", pet.notes)
        })

        root.put("books", serializeList(inMemoryBooks) { b ->
            JSONObject().put("id", b.id).put("title", b.title).put("author", b.author)
                .put("category", b.category).put("totalPages", b.totalPages)
                .put("currentPages", b.currentPages).put("bookshelfLocation", b.bookshelfLocation)
                .put("rating", b.rating).put("borrowerName", b.borrowerName)
                .put("lentDate", b.lentDate).put("notes", b.notes)
                .put("coverPhotoPath", b.coverPhotoPath)
        })

        root.put("beverages", serializeList(inMemoryBeverages) { bv ->
            JSONObject().put("id", bv.id).put("name", bv.name).put("category", bv.category)
                .put("vintageYear", bv.vintageYear).put("optimalAgingYear", bv.optimalAgingYear)
                .put("qty", bv.qty).put("unit", bv.unit).put("storageLocation", bv.storageLocation)
                .put("originRegion", bv.originRegion).put("isOpened", bv.isOpened)
                .put("openedAt", bv.openedAt).put("openedPreserveDays", bv.openedPreserveDays)
                .put("photoPath", bv.photoPath).put("notes", bv.notes)
        })

        root.put("ideas", serializeList(inMemoryIdeas) { idea ->
            val obj = JSONObject().put("id", idea.id).put("content", idea.content)
                .put("voice", idea.voiceMemoPath).put("emoji", idea.moodEmoji)
                .put("pinned", idea.isPinned).put("color", idea.colorHex)
                .put("created_at", idea.createdAt).put("updated_at", idea.updatedAt)
            if (idea.tags.isNotEmpty()) {
                val tArr = JSONArray()
                for (t in idea.tags) tArr.put(t)
                obj.put("tags", tArr)
            }
            if (idea.linkedAssetIds.isNotEmpty()) {
                val lArr = JSONArray()
                for (l in idea.linkedAssetIds) lArr.put(l)
                obj.put("linked_ids", lArr)
            }
            obj
        })

        root.put("clippings", serializeList(inMemoryClippings) { clip ->
            val obj = JSONObject().put("id", clip.id).put("title", clip.title)
                .put("url", clip.originalUrl).put("platform", clip.sourcePlatform)
                .put("markdown", clip.fullMarkdown).put("ocr", clip.ocrRawText)
                .put("summary", clip.summary).put("progress", clip.readingProgress.toDouble())
                .put("archived", clip.isArchived).put("captured_at", clip.capturedAt)
            if (clip.tags.isNotEmpty()) {
                val tArr = JSONArray()
                for (t in clip.tags) tArr.put(t)
                obj.put("tags", tArr)
            }
            if (clip.localImagePaths.isNotEmpty()) {
                val imgArr = JSONArray()
                for (img in clip.localImagePaths) imgArr.put(img)
                obj.put("images", imgArr)
            }
            if (clip.linkedAssetIds.isNotEmpty()) {
                val lArr = JSONArray()
                for (l in clip.linkedAssetIds) lArr.put(l)
                obj.put("linked_ids", lArr)
            }
            obj
        })

        root.put("updatedAt", System.currentTimeMillis())

        val preserved = SnapshotSync.recordChanges(rawSnapshot, root)
        BackupDocument.atomicWrite(dataFile, preserved.toString(2).toByteArray(StandardCharsets.UTF_8))
        rawSnapshot = preserved
    }

    private inline fun <T> serializeList(list: List<T>, mapper: (T) -> JSONObject): JSONArray {
        val arr = JSONArray()
        for (item in list) {
            arr.put(mapper(item))
        }
        return arr
    }

    // ==================== 12 馆 CRUD 门面 ====================

    fun getVouchers(): List<VoucherRecord> = inMemoryVouchers
    fun saveVouchers(list: List<VoucherRecord>) { inMemoryVouchers = list.toMutableList(); saveAll() }
    fun addOrUpdateVoucher(v: VoucherRecord) { updateInList(inMemoryVouchers, v, { it.id == v.id }) }
    fun deleteVoucher(id: String) { inMemoryVouchers.removeAll { it.id == id }; saveAll() }

    fun getIdentityDocs(): List<IdentityDocRecord> = inMemoryIdentityDocs
    fun saveIdentityDocs(list: List<IdentityDocRecord>) { inMemoryIdentityDocs = list.toMutableList(); saveAll() }
    fun addOrUpdateIdentityDoc(i: IdentityDocRecord) { updateInList(inMemoryIdentityDocs, i, { it.id == i.id }) }
    fun deleteIdentityDoc(id: String) { inMemoryIdentityDocs.removeAll { it.id == id }; saveAll() }

    fun getMedicines(): List<MedicineRecord> = inMemoryMedicines
    fun saveMedicines(list: List<MedicineRecord>) { inMemoryMedicines = list.toMutableList(); saveAll() }
    fun addOrUpdateMedicine(m: MedicineRecord) { updateInList(inMemoryMedicines, m, { it.id == m.id }) }
    fun deleteMedicine(id: String) { inMemoryMedicines.removeAll { it.id == id }; saveAll() }

    fun getFoods(): List<FoodRecord> = inMemoryFoods
    fun saveFoods(list: List<FoodRecord>) { inMemoryFoods = list.toMutableList(); saveAll() }
    fun addOrUpdateFood(f: FoodRecord) { updateInList(inMemoryFoods, f, { it.id == f.id }) }
    fun deleteFood(id: String) { inMemoryFoods.removeAll { it.id == id }; saveAll() }

    fun getHonorCredentials(): List<HonorCredentialRecord> = inMemoryHonors
    fun saveHonorCredentials(list: List<HonorCredentialRecord>) { inMemoryHonors = list.toMutableList(); saveAll() }
    fun addOrUpdateHonorCredential(h: HonorCredentialRecord) { updateInList(inMemoryHonors, h, { it.id == h.id }) }
    fun deleteHonorCredential(id: String) { inMemoryHonors.removeAll { it.id == id }; saveAll() }

    fun getWardrobeRecords(): List<WardrobeRecord> = inMemoryWardrobe
    fun saveWardrobeRecords(list: List<WardrobeRecord>) { inMemoryWardrobe = list.toMutableList(); saveAll() }
    fun addOrUpdateWardrobeRecord(w: WardrobeRecord) { updateInList(inMemoryWardrobe, w, { it.id == w.id }) }
    fun deleteWardrobeRecord(id: String) { inMemoryWardrobe.removeAll { it.id == id }; saveAll() }

    fun getEmergencyItems(): List<EmergencyItem> = inMemoryEmergency
    fun saveEmergencyItems(list: List<EmergencyItem>) { inMemoryEmergency = list.toMutableList(); saveAll() }
    fun addOrUpdateEmergencyItem(em: EmergencyItem) { updateInList(inMemoryEmergency, em, { it.id == em.id }) }
    fun deleteEmergencyItem(id: String) { inMemoryEmergency.removeAll { it.id == id }; saveAll() }

    fun getToolRecords(): List<ToolMaintenanceRecord> = inMemoryTools
    fun saveToolRecords(list: List<ToolMaintenanceRecord>) { inMemoryTools = list.toMutableList(); saveAll() }
    fun addOrUpdateToolRecord(t: ToolMaintenanceRecord) { updateInList(inMemoryTools, t, { it.id == t.id }) }
    fun deleteToolRecord(id: String) { inMemoryTools.removeAll { it.id == id }; saveAll() }

    fun getPlantRecords(): List<PlantCareRecord> = inMemoryPlants
    fun savePlantRecords(list: List<PlantCareRecord>) { inMemoryPlants = list.toMutableList(); saveAll() }
    fun addOrUpdatePlantRecord(p: PlantCareRecord) { updateInList(inMemoryPlants, p, { it.id == p.id }) }
    fun deletePlantRecord(id: String) { inMemoryPlants.removeAll { it.id == id }; saveAll() }

    fun getPetRecords(): List<PetCareRecord> = inMemoryPets
    fun savePetRecords(list: List<PetCareRecord>) { inMemoryPets = list.toMutableList(); saveAll() }
    fun addOrUpdatePetRecord(pet: PetCareRecord) { updateInList(inMemoryPets, pet, { it.id == pet.id }) }
    fun deletePetRecord(id: String) { inMemoryPets.removeAll { it.id == id }; saveAll() }

    fun getBookRecords(): List<BookRecord> = inMemoryBooks
    fun saveBookRecords(list: List<BookRecord>) { inMemoryBooks = list.toMutableList(); saveAll() }
    fun addOrUpdateBookRecord(b: BookRecord) { updateInList(inMemoryBooks, b, { it.id == b.id }) }
    fun deleteBookRecord(id: String) { inMemoryBooks.removeAll { it.id == id }; saveAll() }

    fun getBeverageTeaRecords(): List<BeverageTeaRecord> = inMemoryBeverages
    fun saveBeverageTeaRecords(list: List<BeverageTeaRecord>) { inMemoryBeverages = list.toMutableList(); saveAll() }
    fun addOrUpdateBeverageTeaRecord(bv: BeverageTeaRecord) { updateInList(inMemoryBeverages, bv, { it.id == bv.id }) }
    fun deleteBeverageTeaRecord(id: String) { inMemoryBeverages.removeAll { it.id == id }; saveAll() }

    // 💡 闪念想法
    fun getIdeas(): List<IdeaRecord> = inMemoryIdeas
    fun saveIdeas(list: List<IdeaRecord>) { inMemoryIdeas = list.toMutableList(); saveAll() }
    fun addOrUpdateIdea(idea: IdeaRecord) { updateInList(inMemoryIdeas, idea, { it.id == idea.id }) }
    fun deleteIdea(id: String) { inMemoryIdeas.removeAll { it.id == id }; saveAll() }

    // 📰 智能剪藏
    fun getClippings(): List<ClippingRecord> = inMemoryClippings
    fun saveClippings(list: List<ClippingRecord>) { inMemoryClippings = list.toMutableList(); saveAll() }
    fun addOrUpdateClipping(clip: ClippingRecord) { updateInList(inMemoryClippings, clip, { it.id == clip.id }) }
    fun deleteClipping(id: String) { inMemoryClippings.removeAll { it.id == id }; saveAll() }

    private inline fun <T> updateInList(list: MutableList<T>, item: T, predicate: (T) -> Boolean) {
        val idx = list.indexOfFirst(predicate)
        if (idx != -1) {
            list[idx] = item
        } else {
            list.add(0, item)
        }
        saveAll()
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

    @Synchronized
    fun workbenchSnapshot(): JSONObject = JSONObject(dataFile.readText(StandardCharsets.UTF_8))

    @Synchronized
    fun executeWorkbench(command: JSONObject, actor: String = "owner") {
        val result=com.kfaino.collecter.core.CollectionWorkbench.apply(workbenchSnapshot(),command,actor)
        check(importJson(result.toString())) { "整理保存失败，原数据已保留" }
    }

    @Synchronized
    fun exportJson(): String {
        return BackupDocument.attachFiles(JSONObject(dataFile.readText(StandardCharsets.UTF_8)), listOf(dataDir)).toString(2)
    }

    @Synchronized
    fun importJson(jsonStr: String): Boolean {
        val original = dataFile.readBytes()
        var replaced = false
        return try {
            val incoming = WireAliases.convert(BackupDocument.parse(jsonStr))
            val combined = JSONObject(String(original, StandardCharsets.UTF_8))
            for (key in incoming.keys()) combined.put(key, incoming.get(key))
            val restored = BackupDocument.restoreFiles(combined, dataDir)
            BackupDocument.atomicWrite(dataFile, restored.toString(2).toByteArray(StandardCharsets.UTF_8))
            replaced = true
            loadAll()
            true
        } catch (e: Exception) {
            System.err.println("恢复失败，保留原数据: ${e.message}")
            if (replaced) {
                BackupDocument.atomicWrite(dataFile, original)
                loadAll()
            }
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
